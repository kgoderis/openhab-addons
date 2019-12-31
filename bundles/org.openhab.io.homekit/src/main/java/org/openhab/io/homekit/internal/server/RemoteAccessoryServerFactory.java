package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerFactory;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.PairingRegistry;
import org.openhab.io.homekit.internal.client.AccessoryServerConfigurationChangeParticipant;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = { AccessoryServerFactory.class })
@NonNullByDefault
public class RemoteAccessoryServerFactory implements AccessoryServerFactory {

    private final Logger logger = LoggerFactory.getLogger(RemoteAccessoryServerFactory.class);

    @Nullable
    private MDNSService mdnsService;
    @Nullable
    private AccessoryRegistry accessoryRegistry;
    @Nullable
    private PairingRegistry pairingRegistry;
    @Nullable
    private NetworkAddressService networkAddressService;
    @Nullable
    private NotificationRegistry notificationRegistry;
    @Nullable
    private HomekitCommunicationManager communicationManager;
    @Nullable
    private SafeCaller safeCaller;

    protected final @NonNullByDefault({}) BundleContext bundleContext;
    private final Map<AccessoryServerUID, @Nullable ServiceRegistration<?>> mdnsServiceRegs = new HashMap<>();

    @Activate
    public RemoteAccessoryServerFactory(ComponentContext componentContext, @Nullable MDNSService mdnsService,
            @Nullable AccessoryRegistry accessoryRegistry, @Nullable PairingRegistry pairingRegistry,
            @Nullable NetworkAddressService networkAddressService, @Nullable NotificationRegistry notificationRegistry,
            @Nullable HomekitCommunicationManager communicationManager, @Nullable SafeCaller safeCaller) {
        super();
        this.bundleContext = componentContext.getBundleContext();

        this.mdnsService = mdnsService;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.networkAddressService = networkAddressService;
        this.notificationRegistry = notificationRegistry;
        this.communicationManager = communicationManager;
        this.safeCaller = safeCaller;
    }

    @Deactivate
    public void deactivate() {
        for (AccessoryServerUID serverUID : mdnsServiceRegs.keySet()) {
            ServiceRegistration<?> serviceReg = this.mdnsServiceRegs.remove(serverUID);
            if (serviceReg != null) {
                AccessoryServerConfigurationChangeParticipant service = (AccessoryServerConfigurationChangeParticipant) getBundleContext()
                        .getService(serviceReg.getReference());
                serviceReg.unregister();
            }
        }
    }

    @Override
    public @Nullable AccessoryServer createServer(@NonNull String factoryType, InetAddress address, int port) {
        if (Arrays.stream(getSupportedServerTypes()).anyMatch(factoryType::equals)) {
            RemoteStandAloneAccessoryServer newBridge = null;

            try {
                newBridge = new RemoteStandAloneAccessoryServer(address, port, accessoryRegistry, pairingRegistry,
                        notificationRegistry);
                if (newBridge != null) {
                    logger.debug("Created an Accessory Server {} of Type {} running at {}:{}", newBridge.getUID(),
                            newBridge.getClass().getSimpleName(), address.toString(), port);
                    registerHomekitMDNSParticipant(newBridge);

                }
                return newBridge;
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public @Nullable AccessoryServer createServer(@NonNull String factoryType, InetAddress address, int port, byte[] id,
            byte[] privateKey, int configurationIndex) {

        if (Arrays.stream(getSupportedServerTypes()).anyMatch(factoryType::equals)) {
            RemoteStandAloneAccessoryServer newBridge = null;

            try {
                newBridge = new RemoteStandAloneAccessoryServer(address, port, id, privateKey, accessoryRegistry,
                        pairingRegistry, notificationRegistry);
                if (newBridge != null) {
                    logger.debug("Created an Accessory Server {} of Type {} running at {}:{}", newBridge.getUID(),
                            newBridge.getClass().getSimpleName(), address.toString(), port);
                    registerHomekitMDNSParticipant(newBridge);
                }
                return newBridge;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public String @NonNull [] getSupportedServerTypes() {
        return new String[] { RemoteStandAloneAccessoryServer.class.getSimpleName() };
    }

    private synchronized void registerHomekitMDNSParticipant(RemoteStandAloneAccessoryServer participant) {
        AccessoryServerConfigurationChangeParticipant mdnsParticipant = new AccessoryServerConfigurationChangeParticipant(
                participant);
        this.mdnsServiceRegs.put(participant.getUID(), getBundleContext().registerService(
                MDNSDiscoveryParticipant.class.getName(), mdnsParticipant, new Hashtable<String, Object>()));
    }

    protected BundleContext getBundleContext() {
        final BundleContext bundleContext = this.bundleContext;
        if (bundleContext != null) {
            return bundleContext;
        } else {
            throw new IllegalStateException(
                    "The bundle context is missing (it seems your thing handler factory is used but not active).");
        }
    }
}
