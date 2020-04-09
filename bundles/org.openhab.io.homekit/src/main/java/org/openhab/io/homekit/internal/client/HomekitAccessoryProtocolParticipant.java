package org.openhab.io.homekit.internal.client;

public interface HomekitAccessoryProtocolParticipant {

    void updateConfigurationNumber(int configurationNumber);

    int getConfigurationNumber();

    String getAccessoryPairingId();

    void updateDestination(String host, int portNumber);

    void pair(String setupCode);

    void pairVerify();

}