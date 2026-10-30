package org.openhab.binding.atsadvanced.poc.prose;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps cryptic protocol ids / fault codes to end-user English prose for the POC slice.
 */
public final class AtsProse {

    private static final Pattern FAULT_CODE = Pattern.compile("FAULT_[A-Z0-9_]+");

    private final Properties props = new Properties();

    public AtsProse() {
        try (InputStream in = AtsProse.class.getResourceAsStream("/ats-prose.properties")) {
            if (in == null) {
                throw new IllegalStateException("Missing ats-prose.properties");
            }
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load prose catalog", e);
        }
    }

    public String channel(String id) {
        return get("channel." + id, id);
    }

    public String channelDescription(String id) {
        return get("channel." + id + ".description", channel(id));
    }

    public String flag(String protocolFlag) {
        return get("flag." + protocolFlag, humanize(protocolFlag));
    }

    public String state(String key) {
        return get("state." + key, key);
    }

    public String action(String key) {
        return get("action." + key, key);
    }

    /**
     * Prefer structured FAULT_* tokens inside gateway error strings; otherwise generic prose.
     */
    public String faultFromGatewayMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return get("fault.generic", "Unknown error");
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

    public String areaStatusSummary(boolean fullSet, boolean alarm) {
        String arm = fullSet ? state("armed") : state("disarmed");
        String al = alarm ? state("alarmActive") : state("alarmClear");
        return arm + "; " + al;
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
