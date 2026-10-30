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
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * End-user prose for protocol flag and state tokens (not openHAB thing/channel i18n).
 * <p>
 * Maps {@link org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants} flag enum names
 * (e.g. {@code ZNEV_ALARM}) to short English phrases via {@code /ats-prose.properties}. Child thing
 * handlers use this for {@code status} channel text while the bridge talks to
 * {@link org.openhab.binding.atsadvanced.internal.gateway.AtsGatewayClient} over HTTP.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public final class AtsProse {

    private static final AtsProse INSTANCE = new AtsProse();

    private final Properties props = new Properties();

    private AtsProse() {
        try (InputStream in = AtsProse.class.getResourceAsStream("/ats-prose.properties")) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            // fall back to humanize()
        }
    }

    /**
     * @return the singleton prose mapper
     */
    public static AtsProse getInstance() {
        return INSTANCE;
    }

    /**
     * @param protocolFlag raw flag id from a {@code getSTAT.*} property
     * @return readable label for the flag
     */
    public String flag(String protocolFlag) {
        return get("flag." + protocolFlag, humanize(protocolFlag));
    }

    /**
     * @param key state token
     * @return mapped state label
     */
    public String state(String key) {
        return get("state." + key, key);
    }

    /**
     * @param key action token
     * @return mapped action label
     */
    public String action(String key) {
        return get("action." + key, key);
    }

    /**
     * @param protocolFlags active protocol flag ids
     * @return comma-separated readable summary, or “alarm clear” when empty
     */
    public String flagsSummary(Collection<String> protocolFlags) {
        if (protocolFlags.isEmpty()) {
            return state("alarmClear");
        }
        return protocolFlags.stream().map(this::flag).collect(Collectors.joining(", "));
    }

    /**
     * @param raw gateway/panel error text
     * @return delegated to {@link FaultMessages#fromGatewayMessage(String)}
     */
    public String faultFromGatewayMessage(@Nullable String raw) {
        return FaultMessages.getInstance().fromGatewayMessage(raw);
    }

    private String get(String key, String fallback) {
        return Objects.requireNonNullElse(props.getProperty(key), fallback);
    }

    private static String humanize(String id) {
        String s = id;
        for (String prefix : new String[] { "AREV_", "ZNEV_", "OUTEV_", "RASEV_", "DGPEV_", "FAULT_" }) {
            if (s.startsWith(prefix)) {
                s = s.substring(prefix.length());
                break;
            }
        }
        s = s.replace('_', ' ').toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return id;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
