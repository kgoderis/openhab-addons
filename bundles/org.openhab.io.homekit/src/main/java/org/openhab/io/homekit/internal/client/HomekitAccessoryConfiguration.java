package org.openhab.io.homekit.internal.client;

public class HomekitAccessoryConfiguration {

    /**
     * Constants representing the configuration strings
     */
    public static final String HOST_ADDRESS = "hostAddress";
    // public static final String IPV6_ADDRESSES = "ipv6addresses";
    public static final String PORT = "port";
    public static final String CLIENT_PAIRING_ID = "clientPairingId";
    public static final String CLIENT_LTSK = "clientLongTermSecretKey";
    public static final String ACCESSORY_PAIRING_ID = "accessoryPairingId";
    public static final String SETUP_CODE = "setupCode";

    public String hostAddress;
    // public InetAddress[] ipv6Addresses;
    public int port;

}
