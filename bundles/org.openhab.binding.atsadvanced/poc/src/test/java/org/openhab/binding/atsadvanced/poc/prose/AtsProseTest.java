package org.openhab.binding.atsadvanced.poc.prose;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AtsProseTest {

    private final AtsProse prose = new AtsProse();

    @Test
    void mapsAreaFlags() {
        assertEquals("Fully set (armed)", prose.flag("AREV_FULLSET"));
        assertEquals("Alarm", prose.flag("AREV_ALARM"));
    }

    @Test
    void mapsFaultNoAccessFromGatewayBlob() {
        String raw = "Fault response 2, The panel device returned a fault code (FAULT_NO_ACCESS).";
        String text = prose.faultFromGatewayMessage(raw);
        assertTrue(text.contains("Access denied"));
        assertTrue(text.contains("FAULT_NO_ACCESS"));
    }

    @Test
    void areaSummaryUsesProse() {
        assertEquals("Armed; Alarm active", prose.areaStatusSummary(true, true));
        assertEquals("Disarmed; No alarm", prose.areaStatusSummary(false, false));
    }

    @Test
    void channelLabels() {
        assertEquals("Set", prose.channel("set"));
        assertTrue(prose.channelDescription("set").toLowerCase().contains("armed"));
    }
}
