package org.openhab.io.homekit.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;

public class HomekitPersistedAccessoryServer {

    public enum ServerType {
        LOCAL,
        REMOTE
    }

    private String localAddress;
    private String port;
    private String pairingIdentifier;
    private String privateKey;
    private String configurationIndex;
    private String accessories;
    private HomekitAccessoryCategory category;
    private ServerType serverType;

    public HomekitPersistedAccessoryServer() {
        localAddress = "";
        port = "";
        pairingIdentifier = "";
        privateKey = "";
        configurationIndex = "";
        category = HomekitAccessoryCategory.OTHER;
        serverType = ServerType.LOCAL;
    }

    public HomekitPersistedAccessoryServer(@NonNull InetAddress localAddress, int port, byte[] pairingId, byte[] privateKey,
            int configurationIndex, @NonNull Collection<org.openhab.io.homekit.api.accessory.HomekitAccessory> accessories, HomekitAccessoryCategory category,
            ServerType serverType) {
        this.localAddress = localAddress.getHostAddress();
        this.port = Integer.toString(port);
        this.pairingIdentifier = Base64.getEncoder().encodeToString(pairingId);
        this.privateKey = Base64.getEncoder().encodeToString(privateKey);
        this.configurationIndex = Integer.toString(configurationIndex);
        this.category = category;
        this.serverType = serverType;

        // Handle accessories safely
        if (accessories.isEmpty()) {
            this.accessories = "";
        } else {
            this.accessories = accessories.stream().map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(";"));
        }
    }

    public @NonNull InetAddress getLocalAddress() {
        try {
            return InetAddress.getByName(localAddress);
        } catch (UnknownHostException e) {
            return InetAddress.getLoopbackAddress();
        }
    }

    public int getPort() {
        return Integer.parseInt(port);
    }

    public byte[] getPairingIdentifier() {
        return Base64.getDecoder().decode(pairingIdentifier);
    }

    public byte[] getPrivateKey() {
        return Base64.getDecoder().decode(privateKey);
    }

    public int getConfigurationIndex() {
        return Integer.parseInt(configurationIndex);
    }

    public void setLocalAddress(InetAddress localAddress) {
        this.localAddress = localAddress.toString();
    }

    public void setPort(int port) {
        this.port = Integer.toString(port);
    }

    public void setPairingIdentifier(String pairingIdentifier) {
        this.pairingIdentifier = pairingIdentifier;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = Base64.getEncoder().encodeToString(privateKey);
    }

    public void setConfigurationIndex(int configurationIndex) {
        this.configurationIndex = Integer.toString(configurationIndex);
    }

    public void setAccessories(@NonNull Collection<org.openhab.io.homekit.api.accessory.HomekitAccessory> accessories) {
        if (accessories.isEmpty()) {
            this.accessories = "";
        } else {
            this.accessories = accessories.stream().map(accessory -> accessory.getUID().getAsString())
                    .collect(java.util.stream.Collectors.joining(";"));
        }
    }

    public Collection<String> getAccessoryUIDs() {
        if (accessories == null || accessories.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(accessories.split(";"));
    }

    public HomekitAccessoryCategory getCategory() {
        return category;
    }

    public void setCategory(HomekitAccessoryCategory category) {
        this.category = category;
    }

    public ServerType getServerType() {
        return serverType;
    }

    public void setServerType(ServerType serverType) {
        this.serverType = serverType;
    }
}
