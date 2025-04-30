package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public abstract class AbstractHomekitEvent implements HomekitEvent {
    private String sourceUid;
    private final long timestamp;
    private final HomekitEventType type;

    protected AbstractHomekitEvent(String sourceUid, HomekitEventType type) {
        this.sourceUid = sourceUid;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
    }

    @Override
    public String getSourceUid() {
        return sourceUid;
    }

    @Override
    public void setSourceUid(String sourceUid) {
        this.sourceUid = sourceUid;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public HomekitEventType getType() {
        return type;
    }

    @Override
    public abstract String toString();
}
