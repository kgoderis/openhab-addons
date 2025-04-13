package org.openhab.io.homekit.internal.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryCategory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.PairingFeatureFlag;
import org.openhab.io.homekit.api.hap.PairingStatusFlag;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.AccessoryServerRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.internal.provider.HomekitThingTypeProvider;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;
import org.openhab.io.homekit.internal.server.BridgeRemoteAccessoryServer;
import org.openhab.io.homekit.internal.server.StandAloneRemoteAccessoryServer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
@Component(immediate = true, service = DiscoveryService.class, configurationPid = "discovery.homekit")
public class AccessoryServerDiscoveryService extends AbstractDiscoveryService implements ServiceListener {
    private static final Duration FOREGROUND_SCAN_TIMEOUT = Duration.ofMillis(200);
    private static final String SERVICE_TYPE = "_hap._tcp.local.";
    private final Logger logger = LoggerFactory.getLogger(AccessoryServerDiscoveryService.class);

    private final MDNSClient mdnsClient;
    private final AccessoryServerRegistry accessoryServerRegistry;
    private Map<String, ScheduledFuture<?>> deviceRemovalTasks = new ConcurrentHashMap<>();
    private NetworkAddressService networkAddressService;
    private @Nullable MDNSService mdnsService;
    private @Nullable AccessoryRegistry accessoryRegistry;
    private @Nullable PairingRegistry pairingRegistry;
    private @Nullable SafeCaller safeCaller;
    private @Nullable HomekitThingTypeProvider homekitThingTypeProvider;

    @Activate
    public AccessoryServerDiscoveryService(final @Nullable Map<String, Object> configProperties,
            final @Reference MDNSClient mdnsClient, final @Reference AccessoryServerRegistry accessoryServerRegistry,
            final @Reference NetworkAddressService networkAddressService,
            @Nullable MDNSService mdnsService, @Nullable AccessoryRegistry accessoryRegistry,
            @Nullable PairingRegistry pairingRegistry, @Nullable SafeCaller safeCaller,
            @Nullable HomekitThingTypeProvider homekitThingTypeProvider) {
        super(5);
        this.mdnsClient = mdnsClient;
        this.accessoryServerRegistry = accessoryServerRegistry;
        this.networkAddressService = networkAddressService;
        this.mdnsService = mdnsService;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.safeCaller = safeCaller;
        this.homekitThingTypeProvider = homekitThingTypeProvider;
        super.activate(configProperties);

        if (isBackgroundDiscoveryEnabled()) {
            mdnsClient.addServiceListener(SERVICE_TYPE, this);
        }
    }

    @Deactivate
    @Override
    protected void deactivate() {
        super.deactivate();
        mdnsClient.removeServiceListener(SERVICE_TYPE, this);
    }

    @Modified
    @Override
    protected void modified(@Nullable Map<String, Object> configProperties) {
        super.modified(configProperties);
    }

    @Override
    protected void startBackgroundDiscovery() {
        mdnsClient.addServiceListener(SERVICE_TYPE, this);
        startScan(true);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        mdnsClient.removeServiceListener(SERVICE_TYPE, this);
    }

    @Override
    protected void startScan() {
        startScan(false);
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
    }

    private void startScan(boolean isBackground) {
        scheduler.schedule(() -> {
            scan(isBackground);
        }, 0, TimeUnit.SECONDS);
    }

    private void scan(boolean isBackground) {
        long start = System.currentTimeMillis();
        ServiceInfo[] services;
        if (isBackground) {
            services = mdnsClient.list(SERVICE_TYPE);
        } else {
            services = mdnsClient.list(SERVICE_TYPE, FOREGROUND_SCAN_TIMEOUT);
        }
        logger.debug("{} HomeKit services found; duration: {}ms", services.length, System.currentTimeMillis() - start);
        for (ServiceInfo serviceInfo : services) {
            Map<String, String> properties = processService(serviceInfo);

            if (properties == null) {
                continue;
            }

            String deviceId = properties.get("id");
            // creater a AccessoryServerUID from the deviceId
            AccessoryServerUID serverUID = new AccessoryServerUID(deviceId);
            // get the AccessoryServer from the accessoryServerRegistry
            AccessoryServer server = accessoryServerRegistry.get(serverUID);

            if (server == null) {
                continue;
            }

            if (server.isPaired()) {
                logger.info("AccessoryServer {} is paired", server.getUID());

                try {
                    server.updateAccessories();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                for (Accessory accessory : server.getAccessories()) {
                    // Check that the accessory is not already in the accessory registry
                    if (accessoryRegistry.get(accessory.getUID()) == null) {
                        logger.info("Accessory {} is not in the accessory registry", accessory.getUID());
                        accessoryRegistry.add(accessory);
                        createThingFromAccessory(server, accessory);
                        // TODO : add code to check if a things exists for this accessory and if not create one
                        // we need to check all accessories and all services of the accessory
                    } else {
                        logger.info("Accessory {} is already in the accessory registry", accessory.getUID());
                    }
                }

            } else {
                logger.warn("AccessoryServer {} is not paired", server.getUID());
            }

        }
    }

    @Override
    public void serviceAdded(@NonNullByDefault({}) ServiceEvent serviceEvent) {
        considerService(serviceEvent);
    }

    @Override
    public void serviceRemoved(@NonNullByDefault({}) ServiceEvent serviceEvent) {
        ServiceInfo serviceInfo = serviceEvent.getInfo();
        if (serviceInfo != null) {
            AccessoryServerUID serverUID = new AccessoryServerUID(serviceInfo.getName());
            AccessoryServer server = accessoryServerRegistry.get(serverUID);
            if (server != null) {
                long gracePeriod = 30; // 30 seconds grace period
                if (gracePeriod <= 0) {
                    accessoryServerRegistry.remove(serverUID);
                } else {
                    cancelRemovalTask(serviceInfo);
                    scheduleRemovalTask(serverUID, serviceInfo, gracePeriod);
                }
            }
        }
    }

    @Override
    public void serviceResolved(@NonNullByDefault({}) ServiceEvent serviceEvent) {
        considerService(serviceEvent);
    }

    private void considerService(ServiceEvent serviceEvent) {
        if (isBackgroundDiscoveryEnabled()) {
            processService(serviceEvent.getInfo());
        }
    }

    private Map<String, String> processService(ServiceInfo serviceInfo) {
        Map<String, String> properties = new HashMap<>();

        if (serviceInfo == null) {
            return null;
        }

        if (serviceInfo.hasData() && serviceInfo.getApplication().contains("hap") && serviceInfo.getPort() != 0) {
            try {

                Enumeration<String> serviceProperties = serviceInfo.getPropertyNames();
                while (serviceProperties.hasMoreElements()) {
                    String element = serviceProperties.nextElement();
                    String value = serviceInfo.getPropertyString(element);
                    properties.put(element, value);
                }

                String id = serviceInfo.getPropertyString("id");
                if (id == null) {
                    logger.warn("Skipping service with no ID: {}", serviceInfo.getName());
                    return null;
                }

                AccessoryServerUID serverUID = new AccessoryServerUID(id.replace(":", ""));
                AccessoryServer existingServer = accessoryServerRegistry.get(serverUID);

                int port = serviceInfo.getPort();
                String deviceId = serviceInfo.getPropertyString("id");
                String model = serviceInfo.getPropertyString("md");
                String version = serviceInfo.getPropertyString("pv");
                int configIndex = Integer.parseInt(serviceInfo.getPropertyString("c#"));
                AccessoryCategory category = AccessoryCategory
                        .fromValue(Integer.parseInt(serviceInfo.getPropertyString("ci")));
                PairingStatusFlag pairingStatus = PairingStatusFlag
                        .fromValue(Integer.parseInt(serviceInfo.getPropertyString("sf")));
                int stateNumber = Integer.parseInt(serviceInfo.getPropertyString("s#"));
                PairingFeatureFlag pairingFeatureFlag = PairingFeatureFlag
                        .fromValue(Integer.parseInt(serviceInfo.getPropertyString("ff")));

                if (existingServer != null) {
                    // Update configuration index if needed
                    if (existingServer.getConfigurationIndex() != configIndex) {
                        logger.debug("Updating configuration index for server {} from {} to {}",
                                existingServer.getUID(), existingServer.getConfigurationIndex(), configIndex);
                        existingServer.setConfigurationIndex(configIndex);
                    }
                } else {
                    logger.info(
                            "Discovered a Homekit Automation Protocol participant with id '{}', having {} IPv4 and {} IPv6 addresses",
                            id, serviceInfo.getInet4Addresses().length, serviceInfo.getInet6Addresses().length);

                    String hostAddress = null;

                    if (SystemUtils.IS_OS_MAC) {
                        // Use IPv4 only - see
                        // https://medium.com/@quelgar/java-sockets-broken-for-ipv6-on-mac-5aae72f06b21
                        if (networkAddressService.isUseIPv6()) {
                            logger.warn(
                                    "IPv6 and MDNS dot no match well on MacOS - see  https://medium.com/@quelgar/java-sockets-broken-for-ipv6-on-mac-5aae72f06b21");
                        }
                        if (serviceInfo.getInet4Addresses().length > 0) {
                            hostAddress = serviceInfo.getInet4Addresses()[0].getHostAddress();
                        }
                    } else {
                        if (networkAddressService.isUseIPv6()) {
                            if (serviceInfo.getInet6Addresses().length > 0) {
                                hostAddress = serviceInfo.getInet6Addresses()[0].getHostAddress();
                            }
                        } else {
                            if (serviceInfo.getInet4Addresses().length > 0) {
                                hostAddress = serviceInfo.getInet4Addresses()[0].getHostAddress();
                            }
                        }
                    }

                    if (hostAddress == null) {
                        logger.warn(
                                "Skipping a discovered Homekit Automation Protocol participant without valid host address");
                        return null;
                    }

                    logger.info(
                            "Found a Homekit Accessory Server with id {}, category {}, model {}, version {}, configuration index {}, pairing status {}, pairing feature flag {}",
                            id, category, model, version, configIndex, pairingStatus, pairingFeatureFlag);

                    try {
                        AccessoryServer server = null;
                        if (category.equals(AccessoryCategory.BRIDGES)) {
                            server = new BridgeRemoteAccessoryServer(InetAddress.getByName(hostAddress), port,
                                    mdnsService, accessoryRegistry, pairingRegistry, safeCaller);
                        } else {
                            server = new StandAloneRemoteAccessoryServer(InetAddress.getByName(hostAddress), port,
                                    accessoryRegistry, pairingRegistry);
                        }
                        server.setConfigurationIndex(configIndex);
                        accessoryServerRegistry.add(server);
                        logger.debug("Created a Remote Accessory Server {} with Setup Code {}", server.getUID(),
                                server.getSetupCode());
                    } catch (Exception e) {
                        logger.error("Error creating accessory server", e);
                    }

                }

                cancelRemovalTask(serviceInfo);
                return properties;
            } catch (Exception e) {
                logger.error("Error processing HomeKit service: {}", serviceInfo.getName(), e);
            }
        }
        return properties;
    }

    private void cancelRemovalTask(ServiceInfo serviceInfo) {
        ScheduledFuture<?> deviceRemovalTask = deviceRemovalTasks.remove(serviceInfo.getQualifiedName());
        if (deviceRemovalTask != null) {
            deviceRemovalTask.cancel(false);
        }
    }

    private void scheduleRemovalTask(AccessoryServerUID serverUID, ServiceInfo serviceInfo, long gracePeriod) {
        deviceRemovalTasks.put(serviceInfo.getQualifiedName(), scheduler.schedule(() -> {
            accessoryServerRegistry.remove(serverUID);
            cancelRemovalTask(serviceInfo);
        }, gracePeriod, TimeUnit.SECONDS));
    }

    private void createThingFromAccessory(AccessoryServer server, Accessory accessory) {
        // TODO: Implement thing creation from accessory
        logger.debug("Creating thing from accessory {}", accessory.getUID());

        Map<String, Object> properties = new HashMap<>();

        // Get the ThingTypeUID for each service in the accessory
        Collection<Service> services = accessory.getServices();
        for (Service service : services) {
            String serviceType = service.getInstanceType();
            ThingTypeUID thingTypeUID = homekitThingTypeProvider.getThingTypeUID(serviceType);

            // Create a unique ID for this accessory's service
            ThingUID thingUID = new ThingUID(thingTypeUID,
                    "service-" + accessory.getAccessoryId() + "-" + service.getInstanceId());

            // Build discovery result
            DiscoveryResultBuilder builder = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withProperty("accessoryId", accessory.getAccessoryId())
                    .withProperty("instanceId", service.getInstanceId())
                    .withProperty("deviceId", new String(server.getPairingId(), StandardCharsets.UTF_8))
                    .withLabel("HomeKit " + service.getClass().getSimpleName());

            thingDiscovered(builder.build());

            logger.debug("Created thing {} for HomeKit service type {}", thingUID, serviceType);
        }
    }
}
