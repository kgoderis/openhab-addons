package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface HomekitEvent {
    String getSourceUid();

    void setSourceUid(String uid);

    long getTimestamp();

    HomekitEventType getType();
}
