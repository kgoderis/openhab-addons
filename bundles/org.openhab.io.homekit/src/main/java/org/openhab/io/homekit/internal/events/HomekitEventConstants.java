package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class HomekitEventConstants {
    public static final String EVENT_THREAD_NAME = "homekit-event-dispatcher";
    public static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    public static final String POWER = "power";
    public static final String BRIGHTNESS = "brightness";
    public static final String TEMPERATURE = "temperature";
    public static final String HUMIDITY = "humidity";
}
