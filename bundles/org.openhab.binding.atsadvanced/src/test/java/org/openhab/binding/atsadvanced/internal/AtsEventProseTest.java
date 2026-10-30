/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 */
package org.openhab.binding.atsadvanced.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.openhab.binding.atsadvanced.internal.PanelClient.Property;

/**
 * Ensures the extracted event map covers protocol ids with usable prose.
 */
public class AtsEventProseTest {

    @Test
    public void mapsRemoteProgrammingBegin() {
        AtsEventProse prose = AtsEventProse.getInstance();
        assertThat(prose.eventSymbol(74), is("ev_RB"));
        assertThat(prose.event(74), containsString("Remote programming begin"));
        assertThat(prose.source(10), is("System"));
    }

    @Test
    public void coversFullEventIdRange() {
        AtsEventProse prose = AtsEventProse.getInstance();
        for (int i = 0; i <= 219; i++) {
            assertThat("event." + i, prose.event(i), not(startsWith("Event ")));
            assertThat("eventSymbol." + i, prose.eventSymbol(i), startsWith("ev_"));
        }
    }

    @Test
    public void formatsMonitorLine() {
        ArrayList<Property> props = new ArrayList<>();
        props.add(prop("timeStamp", "2026-10-30T10:28:21+01:00"));
        props.add(prop("unique_id", 40809));
        props.add(prop("event_ID", 74));
        props.add(prop("event_source", 10));
        props.add(prop("source_ID", 1));
        props.add(prop("Area", 0));
        props.add(prop("event_text", "Goderis Karel"));

        String line = AtsEventProse.getInstance().formatProperties(props);
        assertThat(line, containsString("Remote programming begin"));
        assertThat(line, containsString("[ev_RB]"));
        assertThat(line, containsString("System #1"));
        assertThat(line, containsString("Goderis Karel"));
        assertThat(line, containsString("40809"));
        assertThat(line, not(containsString("event type 74")));
    }

    private static Property prop(String id, Object value) {
        Property p = new Property();
        p.setId(id);
        p.setIndex(0);
        p.setValue(value);
        return p;
    }
}
