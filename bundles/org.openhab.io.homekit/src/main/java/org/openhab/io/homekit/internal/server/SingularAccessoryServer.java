package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.PairingRegistry;

public class SingularAccessoryServer extends AbstractRemoteAccessoryServer {

    public SingularAccessoryServer(InetAddress address, int port, byte[] pairingIdentifier, byte[] secretKey,
            AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry,
            NotificationRegistry notificationRegistry) {
        super(address, port, pairingIdentifier, secretKey, accessoryRegistry, pairingRegistry, notificationRegistry);
        // TODO Auto-generated constructor stub
    }

    @Override
    public @NonNull AccessoryServerUID getUID() {
        return new AccessoryServerUID("Singular", getId());
    }

    @Override
    public @Nullable ManagedAccessory getAccessory(int instanceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public @Nullable ManagedAccessory getAccessory(@NonNull Class<? extends @NonNull ManagedAccessory> accessoryClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addAccessory(@NonNull ManagedAccessory accessory) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAccessory(@NonNull ManagedAccessory accessory) {
        // TODO Auto-generated method stub

    }

}
