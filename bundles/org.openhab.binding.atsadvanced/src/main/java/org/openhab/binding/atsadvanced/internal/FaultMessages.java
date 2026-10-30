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
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Maps cryptic panel/gateway {@code FAULT_*} / {@code AREV_*} tokens to short English text.
 * Thing/channel labels stay in OH-INF i18n — this is protocol vocabulary only.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public final class FaultMessages {

    private static final Pattern FAULT_CODE = Pattern.compile("FAULT_[A-Z0-9_]+");
    private static final FaultMessages INSTANCE = new FaultMessages();

    private final Properties props = new Properties();

    private FaultMessages() {
        try (InputStream in = FaultMessages.class.getResourceAsStream("/fault-messages.properties")) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            // fall back to humanize-only
        }
    }

    public static FaultMessages getInstance() {
        return INSTANCE;
    }

    public String flag(String protocolFlag) {
        return get("flag." + protocolFlag, humanize(protocolFlag));
    }

    public String fromGatewayMessage(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return get("fault.generic", "Unknown panel or gateway error");
        }
        Matcher m = FAULT_CODE.matcher(raw);
        if (m.find()) {
            String code = m.group();
            String mapped = props.getProperty("fault." + code);
            if (mapped != null) {
                return mapped + " [" + code + "]";
            }
            return humanize(code) + " [" + code + "]";
        }
        return get("fault.generic", "Panel or gateway error") + ": " + raw;
    }

    private String get(String key, String fallback) {
        return Objects.requireNonNullElse(props.getProperty(key), fallback);
    }

    private static String humanize(String id) {
        String s = id;
        if (s.startsWith("AREV_")) {
            s = s.substring(5);
        }
        if (s.startsWith("FAULT_")) {
            s = s.substring(6);
        }
        s = s.replace('_', ' ').toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return id;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
