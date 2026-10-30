/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.atsadvanced.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.atsadvanced.internal.PanelClient.Property;

/**
 * Translates ATS panel event property bags into a single human-readable line for UI channels and logs.
 * <p>
 * Numeric event, source, and log-type identifiers are resolved from {@code /ats-events.properties}
 * on the classpath. This layer sits entirely in the Java binding: the dumb gateway forwards opaque
 * {@link org.openhab.binding.atsadvanced.internal.PanelClient.Property} lists from NAce/.drv without
 * interpreting them.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public final class AtsEventProse {

    private static final AtsEventProse INSTANCE = new AtsEventProse();
    private static final DateTimeFormatter LOCAL_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Properties props = new Properties();

    private AtsEventProse() {
        try (InputStream in = AtsEventProse.class.getResourceAsStream("/ats-events.properties")) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            // fall back to raw ids
        }
    }

    /**
     * @return the singleton prose mapper
     */
    public static AtsEventProse getInstance() {
        return INSTANCE;
    }

    /**
     * @param eventId panel event identifier
     * @return localized event title or a fallback {@code Event <id>}
     */
    public String event(int eventId) {
        return get("event." + eventId, "Event " + eventId);
    }

    /**
     * @param eventId panel event identifier
     * @return short symbol label for the event
     */
    public String eventSymbol(int eventId) {
        return get("eventSymbol." + eventId, "ev_" + eventId);
    }

    /**
     * @param source event source type id
     * @return source category label
     */
    public String source(int source) {
        return get("source." + source, "Source " + source);
    }

    /**
     * @param type log type id
     * @return log category label
     */
    public String logType(int type) {
        return get("logType." + type, "Log type " + type);
    }

    /**
     * Build a single readable line for monitor / log UI.
     */
    public String format(@Nullable String timeStampText, int uniqueId, int eventId, int eventSource, int sourceId,
            int area, @Nullable String eventText, @Nullable Integer logType) {
        StringBuilder sb = new StringBuilder();
        sb.append(timeStampText != null && !timeStampText.isBlank() ? timeStampText : "unknown time");
        sb.append(" — ").append(event(eventId));
        sb.append(" [").append(eventSymbol(eventId)).append(']');

        if (eventSource > 0) {
            sb.append(" · ").append(source(eventSource));
            if (sourceId > 0) {
                sb.append(" #").append(sourceId);
            }
        } else if (sourceId > 0) {
            sb.append(" · #").append(sourceId);
        }

        if (area > 0) {
            sb.append(" · Area ").append(area);
        }

        if (logType != null) {
            sb.append(" · ").append(logType(logType));
        }

        if (eventText != null && !eventText.isBlank()) {
            sb.append(" — ").append(eventText.trim());
        }

        if (uniqueId > 0) {
            sb.append(" (id ").append(uniqueId).append(')');
        }
        return sb.toString();
    }

    /**
     * @param properties monitor or {@code return.getLOG} property bag
     * @return single-line formatted event
     */
    public String formatProperties(ArrayList<Property> properties) {
        @Nullable
        String timeStampText = null;
        int uniqueID = 0;
        int eventID = 0;
        int eventSource = 0;
        int sourceID = 0;
        int area = 0;
        @Nullable
        Integer logType = null;
        @Nullable
        String eventText = null;

        for (Property aProperty : properties) {
            if (aProperty.getId() == null) {
                continue;
            }
            switch (aProperty.getId()) {
                case "timeStamp" -> {
                    if (aProperty.getValue() != null) {
                        String raw = String.valueOf(aProperty.getValue());
                        Date parsed = parseTimeStamp(raw);
                        timeStampText = parsed != null ? LOCAL_TS.format(parsed.toInstant()) : raw;
                    }
                }
                case "unique_id" -> uniqueID = toInt(aProperty.getValue());
                case "event_ID" -> eventID = toInt(aProperty.getValue());
                case "event_source" -> eventSource = toInt(aProperty.getValue());
                case "source_ID" -> sourceID = toInt(aProperty.getValue());
                case "Area" -> {
                    if (aProperty.getValue() != null) {
                        area = toInt(aProperty.getValue());
                    }
                }
                case "logType" -> logType = Integer.valueOf(toInt(aProperty.getValue()));
                case "event_text" -> {
                    Object text = aProperty.getValue();
                    if (text != null) {
                        eventText = String.valueOf(text);
                    }
                }
                default -> {
                }
            }
        }
        return format(timeStampText, uniqueID, eventID, eventSource, sourceID, area, eventText, logType);
    }

    private static @Nullable Date parseTimeStamp(String ts) {
        try {
            return Date.from(OffsetDateTime.parse(ts).toInstant());
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return Date.from(LocalDateTime.parse(ts).atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        DateTimeFormatter[] legacy = { DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss", Locale.US),
                DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss", Locale.US),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.UK),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT) };
        for (DateTimeFormatter fmt : legacy) {
            try {
                return Date.from(LocalDateTime.parse(ts, fmt).atZone(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    private String get(String key, String fallback) {
        String v = props.getProperty(key);
        return v != null ? v : fallback;
    }

    private static int toInt(@Nullable Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            return (int) Double.parseDouble(s);
        }
        return 0;
    }
}
