package org.openhab.io.homekit.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface AccessoryServerChangeListener {

    void updated(AccessoryServer server);

}
