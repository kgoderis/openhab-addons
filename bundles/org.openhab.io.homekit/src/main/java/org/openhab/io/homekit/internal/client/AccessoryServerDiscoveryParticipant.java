package org.openhab.io.homekit.internal.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jmdns.ServiceInfo;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerFactory;
import org.openhab.io.homekit.api.AccessoryServerRegistry;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;
import org.openhab.io.homekit.internal.server.RemoteStandAloneAccessoryServer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = MDNSDiscoveryParticipant.class, immediate = true)
public class AccessoryServerDiscoveryParticipant implements MDNSDiscoveryParticipant {

    private final Logger logger = LoggerFactory.getLogger(AccessoryServerDiscoveryParticipant.class);

    private static final String HAP_SERVICE_TYPE = "_hap._tcp.local.";

    private final AccessoryServerRegistry accessoryServerRegistry;
    private final NetworkAddressService networkAddressService;
    private final Map<String, ThingUID> cachedServices;
    private final Collection<AccessoryServerFactory> serverFactories = new CopyOnWriteArrayList<>();

    @Activate
    public AccessoryServerDiscoveryParticipant(@Reference AccessoryServerRegistry accessoryServerRegistry,
            @Reference NetworkAddressService networkAddressService) {
        this.accessoryServerRegistry = accessoryServerRegistry;
        this.networkAddressService = networkAddressService;
        this.cachedServices = new HashMap<String, ThingUID>();
    }

    @Override
    public @NonNull Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.emptySet();
    }

    @Override
    public @NonNull String getServiceType() {
        return HAP_SERVICE_TYPE;
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    public void addServerFactory(AccessoryServerFactory serverFactory) {
        serverFactories.add(serverFactory);

        logger.debug("Added an Accessory Server Factory that supports {}",
                Arrays.toString(serverFactory.getSupportedServerTypes()));
    }

    public void removeServerFactory(AccessoryServerFactory serverFactory) {
        serverFactories.remove(serverFactory);
    }

    @Override
    public @Nullable DiscoveryResult createResult(@NonNull ServiceInfo service) {

        if (service.hasData() && service.getApplication().contains("hap") && service.getPort() != 0) {

            String id = service.getPropertyString("id");
            if (id != null) {
                logger.info(
                        "Discovered a Homekit Automation Protocol participant with id '{}', having {} IPv4 and {} IPv6 addresses",
                        id, service.getInet4Addresses().length, service.getInet6Addresses().length);

                String hostAddress = null;
                int port = 0;

                Map<String, Object> properties = new HashMap<>();

                if (SystemUtils.IS_OS_MAC) {
                    // Use IPv4 only - see
                    // https://medium.com/@quelgar/java-sockets-broken-for-ipv6-on-mac-5aae72f06b21
                    if (networkAddressService.isUseIPv6()) {
                        logger.warn(
                                "IPv6 and MDNS dot no match well on MacOS - see  https://medium.com/@quelgar/java-sockets-broken-for-ipv6-on-mac-5aae72f06b21");
                    }
                    if (service.getInet4Addresses().length > 0) {
                        hostAddress = service.getInet4Addresses()[0].getHostAddress();
                    }
                } else {
                    if (networkAddressService.isUseIPv6()) {
                        if (service.getInet6Addresses().length > 0) {
                            hostAddress = service.getInet6Addresses()[0].getHostAddress();
                        }
                    } else {
                        if (service.getInet4Addresses().length > 0) {
                            hostAddress = service.getInet4Addresses()[0].getHostAddress();
                        }
                    }
                }

                if (hostAddress == null) {
                    logger.warn(
                            "Skipping a discovered Homekit Automation Protocol participant without valid host address");
                    return null;
                }

                port = service.getPort();

                boolean alreadyExists = false;
                for (AccessoryServerFactory factory : serverFactories) {
                    for (String type : factory.getSupportedServerTypes()) {
                        AccessoryServer accessoryServer = accessoryServerRegistry
                                .get(new AccessoryServerUID(type, id.replace(":", "")));
                        if (accessoryServer != null) {
                            logger.debug(
                                    "The Accessory Server Registry already contains an Accessory Server with Id '{}'",
                                    accessoryServer.getUID());
                            alreadyExists = true;
                            break;
                        }
                    }
                }

                if (!alreadyExists) {
                    for (AccessoryServerFactory factory : serverFactories) {
                        AccessoryServer server = null;
                        try {
                            server = factory.createServer(RemoteStandAloneAccessoryServer.class.getSimpleName(),
                                    InetAddress.getByName(hostAddress), port);
                        } catch (UnknownHostException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        if (server != null) {
                            accessoryServerRegistry.add(server);
                            logger.debug("Created a Remote Accessory Server {} with Setup Code {}", server.getUID(),
                                    server.getSetupCode());
                        } else {
                            logger.warn("Unable to create an Accessory Server of Type {}",
                                    RemoteStandAloneAccessoryServer.class.getSimpleName());
                        }
                    }
                }

                Enumeration<String> serviceProperties = service.getPropertyNames();
                while (serviceProperties.hasMoreElements()) {
                    String element = serviceProperties.nextElement();
                    String value = service.getPropertyString(element);
                    // properties.put(element, value);
                    //
                    // if (element.equals(HomekitBindingConstants.CONFIGURATION_NUMBER_SHARP)) {
                    // properties.put(HomekitAccessoryConfiguration.CONFIGURATION_NUMBER, value);
                    // }

                    if (element.equals(HomekitBindingConstants.DEVICE_ID)) {
                        properties.put(HomekitAccessoryConfiguration.ACCESSORY_PAIRING_ID,
                                Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
                    }
                }

                ThingUID uid = getThingUID(service);
                cachedServices.put(service.getQualifiedName(), uid);

                if (uid != null) {

                    DiscoveryResultBuilder builder = DiscoveryResultBuilder.create(uid).withProperties(properties)
                            .withRepresentationProperty(HomekitBindingConstants.DEVICE_ID);

                    String category = service.getPropertyString("ci");

                    if (category.equals("2")) {
                        return builder.withLabel("Homekit Accessory Bridge").build();
                    } else {
                        return builder.withLabel("Homekit StandAlone Accessory").build();
                    }
                }
            }
        }

        return null;
    }

    @Override
    public @Nullable ThingUID getThingUID(@NonNull ServiceInfo service) {
        if (service.hasData()) {
            if (service.getApplication().contains("hap") && service.getPropertyString("id") != null
                    && service.getPropertyString("ci") != null) {
                String id = service.getPropertyString("id").replace(":", "");

                if (service.getPropertyString("ci").contentEquals("2")) {
                    return new ThingUID(HomekitBindingConstants.THING_TYPE_BRIDGE, id);
                } else {
                    return new ThingUID(HomekitBindingConstants.THING_TYPE_STANDALONE_ACCESSORY, id);
                }
            }
        } else {
            if (service.getApplication().contains("hap") && cachedServices.containsKey(service.getQualifiedName())) {
                logger.warn("Removing {} from the service cache", service.getQualifiedName());
                ThingUID thingUID = cachedServices.remove(service.getQualifiedName());
                return thingUID;
            }
        }

        return null;
    }
}
