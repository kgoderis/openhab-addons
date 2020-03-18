package org.openhab.io.homekit.internal.client;

import java.net.InetAddress;

public class HomekitAccessoryConfiguration {

    /**
     * Constants representing the configuration strings
     */
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String CLIENT_PAIRING_ID = "clientPairingId";
    public static final String CLIENT_LTSK = "clientLongTermSecretKey";
    public static final String ACCESSORY_PAIRING_ID = "accessoryPairingId";
    public static final String SETUP_CODE = "setupCode";
    public static final String CONFIGURATION_NUMBER = "configurationNumber";

    public InetAddress host;
    public int port;
    public byte[] clientPairingId;
    public byte[] clientLongTermSecrectKey;
    public byte[] accessoryPairingId;
    public String setupCode;
    public int configurationNumber;

}
