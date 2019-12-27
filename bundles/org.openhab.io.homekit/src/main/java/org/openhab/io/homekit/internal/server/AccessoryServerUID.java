package org.openhab.io.homekit.internal.server;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;

@NonNullByDefault
public class AccessoryServerUID extends UID {

    // type : server mac address

    @Override
    protected int getMinimalNumberOfSegments() {
        return 2;
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param type the server type
     * @param serverId the server id
     */
    public AccessoryServerUID(String serverType, String serverId) {
        super(serverType, serverId);
    }

    /**
     * Returns the id.
     *
     * @return id the id
     */
    public String getId() {
        List<String> segments = getAllSegments();
        return segments.get(segments.size() - 1);
    }

}
