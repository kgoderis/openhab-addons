package org.openhab.io.homekit.internal.client;

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

    public String host;
    public int port;
    public String clientPairingId;
    public String clientLongTermSecretKey;
    public String accessoryPairingId;
    public String setupCode;
    public int configurationNumber;

}
