package org.openhab.io.homekit.internal.server;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;

import org.eclipse.jdt.annotation.NonNull;

public class PersistedAccessoryServer {

    private String localAddress;
    private String port;
    private String pairingIdentifier;
    private String salt;
    private String privateKey;
    private String configurationIndex;

    public PersistedAccessoryServer() {
        localAddress = "";
        port = "";
        pairingIdentifier = "";
        salt = "";
        privateKey = "";
        configurationIndex = "";
    }

    public PersistedAccessoryServer(@NonNull InetAddress localAddress, int port, String id, BigInteger salt,
            byte[] privateKey, int configurationIndex) {
        this.localAddress = localAddress.getHostAddress();
        this.port = Integer.toString(port);
        this.pairingIdentifier = id;
        this.salt = salt.toString();
        this.privateKey = Base64.getEncoder().encodeToString(privateKey);
        this.configurationIndex = Integer.toString(configurationIndex);
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

    public String getPairingIdentifier() {
        return pairingIdentifier;
    }

    public BigInteger getSalt() {
        return new BigInteger(salt);
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

    public void setSalt(BigInteger salt) {
        this.salt = salt.toString();
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = Base64.getEncoder().encodeToString(privateKey);
    }

    public void setConfigurationIndex(int configurationIndex) {
        this.configurationIndex = Integer.toString(configurationIndex);
    }

}
