package org.openhab.io.homekit.internal.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.listener.AccessoryServerChangeListener;
import org.openhab.io.homekit.api.listener.CharacteristicChangeListener;
import org.openhab.io.homekit.api.listener.ServiceChangeListener;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.AccessoryServerRegistry;
import org.openhab.io.homekit.internal.accessory.AccessoryUID;
import org.openhab.io.homekit.internal.client.HomekitException;
import org.openhab.io.homekit.internal.events.AccessoryServerEvent;
import org.openhab.io.homekit.internal.events.CharacteristicEvent;
import org.openhab.io.homekit.internal.events.ServiceEvent;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class ServiceThingHandler extends BaseThingHandler
        implements AccessoryServerChangeListener, ServiceChangeListener, CharacteristicChangeListener {
    private final Logger logger = LoggerFactory.getLogger(ServiceThingHandler.class);
    private final AccessoryServerRegistry serverRegistry;
    private final AccessoryRegistry accessoryRegistry;
    private final String deviceId;
    private final String serviceId;
    private final String accessoryId;
    private AccessoryServer server;
    private Accessory accessory;
    private Service service;
    private final Map<Channel, Characteristic<?>> characteristicMap = new ConcurrentHashMap<>();

    public ServiceThingHandler(Thing thing, AccessoryServerRegistry serverRegistry,
            AccessoryRegistry accessoryRegistry) {
        super(thing);
        this.serverRegistry = serverRegistry;
        this.accessoryRegistry = accessoryRegistry;

        // Parse configuration
        Configuration config = thing.getConfiguration();
        deviceId = (String) config.get("deviceId");
        serviceId = (String) config.get("serviceId");
        Object accessoryIdConfig = config.get("accessoryId");
        accessoryId = accessoryIdConfig != null ? (String) accessoryIdConfig : "1";

        if (deviceId == null || serviceId == null) {
            throw new IllegalArgumentException("Thing configuration must contain deviceId and serviceId parameters");
        }

        // Get the server from registry
        AccessoryServer foundServer = this.serverRegistry.get(new AccessoryServerUID(deviceId));
        if (foundServer == null) {
            throw new IllegalArgumentException("No AccessoryServer found for deviceId: " + deviceId);
        }
        this.server = foundServer;

        // Get the accessory from registry
        Accessory foundAccessory = this.accessoryRegistry.get(new AccessoryUID(accessoryId));
        if (foundAccessory == null) {
            throw new IllegalArgumentException("No Accessory found for accessoryId: " + accessoryId);
        }
        this.accessory = foundAccessory;

        // Get the service from accessory
        Service foundService = accessory.getService(serviceId);
        if (foundService == null) {
            throw new IllegalArgumentException("No Service found for serviceId: " + serviceId);
        }
        this.service = foundService;

        // Verify ThingType matches Service type
        String thingType = thing.getThingTypeUID().getId();
        String serviceTag;
        try {
            serviceTag = getServiceTag(service);
            if (!thingType.equals(serviceTag)) {
                throw new IllegalArgumentException(
                        "ThingType " + thingType + " does not match Service type " + serviceTag);
            }
        } catch (HomekitException e) {
            throw new IllegalArgumentException("Service type could not be determined", e);
        }

        // Compare and map Channels with Characteristics
        if (thing.getChannels().isEmpty()) {
            // If no channels configured, add all characteristics as channels
            for (Characteristic<?> characteristic : service.getCharacteristics()) {
                String channelId = characteristic.getUID().toString();
                addChannelForCharacteristic(characteristic);
                characteristicMap.put(thing.getChannel(new ChannelUID(thing.getUID(), channelId)), characteristic);
            }
        } else {
            // Compare existing channels with characteristics
            for (Channel channel : thing.getChannels()) {
                String channelId = channel.getUID().getId();
                Characteristic<?> characteristic = null;
                for (Characteristic<?> c : service.getCharacteristics()) {
                    try {
                        if (getCharacteristicTag(c).equals(channelId)) {
                            characteristic = c;
                            break;
                        }
                    } catch (HomekitException e) {
                        logger.warn("Channel {} has no matching Characteristic in Service {}", channelId,
                                service.getUID());
                    }
                }

                if (characteristic == null) {
                    logger.warn("Channel {} has no matching Characteristic in Service {}", channelId, service.getUID());
                } else {
                    characteristicMap.put(channel, characteristic);
                }
            }
        }

        // Register as listener
        this.server.addChangeListener(this);
        this.service.addChangeListener(this);

        initializeChannels();
    }

    private String getServiceTag(Service service) throws HomekitException {
        try {
            Class<?> serviceClass = service.getClass();
            java.lang.reflect.Method getTagMethod = serviceClass.getMethod("getTag");
            return (String) getTagMethod.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            throw new HomekitException(
                    "Service " + service.getClass().getName() + " does not implement getTag() method");
        }
    }

    private String getCharacteristicTag(Characteristic<?> characteristic) throws HomekitException {
        try {
            Class<?> characteristicClass = characteristic.getClass();
            java.lang.reflect.Method getTagMethod = characteristicClass.getMethod("getTag");
            return (String) getTagMethod.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            throw new HomekitException(
                    "Characteristic " + characteristic.getClass().getName() + " does not implement getTag() method");
        }
    }

    @Override
    public void initialize() {
        try {
            // Start the server if it's not already running
            if (!server.isPaired()) {
                server.advertise();
                server.start();
            }

            updateStatus(ThingStatus.ONLINE);
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    @Override
    public void dispose() {
        // Unregister listeners
        this.server.removeChangeListener(this);
        this.service.removeChangeListener(this);
        characteristicMap.values().forEach(characteristic -> characteristic.removeChangeListener(this));
        super.dispose();
    }

    @Override
    public void onAccessoryServerEvent(AccessoryServerEvent event) {
        if (event.getServer().equals(server)) {
            switch (event.getType()) {
                case SERVER_STATE_CONNECTED:
                    updateStatus(ThingStatus.ONLINE);
                    break;
                case SERVER_STATE_DISCONNECTED:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Server disconnected");
                    break;
                case SERVER_STATE_PAIRED:
                case SERVER_STATE_PAIR_VERIFIED:
                    updateStatus(ThingStatus.ONLINE);
                    break;
                case SERVER_STATE_UNPAIRED:
                case SERVER_STATE_PAIR_UNVERIFIED:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Server not paired");
                    break;
                case SERVER_STATE_MISSING_SETUP_CODE:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Missing setup code");
                    break;
                case SERVER_STATE_PAIRING_MISSING:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "Pairing information missing");
                    break;
                case SERVICE_ADDED:
                    if (event.getService().equals(service)) {
                        updateStatus(ThingStatus.ONLINE);
                    }
                    break;
                case SERVICE_REMOVED:
                    if (event.getService().equals(service)) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "Service was removed");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onServiceEvent(ServiceEvent serviceEvent) {
        if (serviceEvent.getService().equals(service)) {
            switch (serviceEvent.getType()) {
                case CHARACTERISTIC_ADDED:
                    addChannelForCharacteristic(serviceEvent.getCharacteristic());
                    break;
                case CHARACTERISTIC_REMOVED:
                    removeChannelForCharacteristic(serviceEvent.getCharacteristic());
                    break;
                case CHARACTERISTIC_STATE_CHANGED:
                    // TODO: Handle characteristic state changed
                    break;
            }
        }
    }

    @Override
    public void onCharacteristicEvent(CharacteristicEvent event) {
        Characteristic<?> characteristic = event.getCharacteristic();
        String channelId = characteristic.getUID().toString();
        ChannelUID channelUID = new ChannelUID(thing.getUID(), channelId);
        Channel channel = thing.getChannel(channelUID);

        if (channel != null && characteristicMap.get(channel) == characteristic) {
            Object newValue = event.getNewValue();
            if (newValue instanceof State) {
                updateState(channelUID, (State) newValue);
            }
        }
    }

    private void initializeChannels() {
        service.getCharacteristics().forEach(characteristic -> {
            String channelId = characteristic.getUID().toString();
            Channel channel = addChannelForCharacteristic(characteristic);
            characteristicMap.put(channel, characteristic);
            characteristic.addChangeListener(this);
        });
    }

    private Channel addChannelForCharacteristic(Characteristic<?> characteristic) {
        String channelId = characteristic.getUID().toString();
        ChannelUID channelUID = new ChannelUID(thing.getUID(), channelId);

        Channel channel = ChannelBuilder.create(channelUID, characteristic.getUID().toString())
                .withType(new ChannelTypeUID("homekit", characteristic.getUID().toString()))
                .withLabel(characteristic.getDescription()).withDescription(characteristic.getDescription()).build();

        updateThing(editThing().withChannel(channel).build());
        return channel;
    }

    private void removeChannelForCharacteristic(Characteristic<?> characteristic) {
        String channelId = characteristic.getUID().toString();
        ChannelUID channelUID = new ChannelUID(thing.getUID(), channelId);
        Channel channel = thing.getChannel(channelUID);
        if (channel != null) {
            characteristicMap.remove(channel);
            updateThing(editThing().withoutChannel(channelUID).build());
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        Channel channel = thing.getChannel(channelUID);
        if (channel != null) {
            Characteristic<?> characteristic = characteristicMap.get(channel);
            if (characteristic != null) {
                try {
                    handleCharacteristicCommand(characteristic, command);
                } catch (Exception e) {
                    logger.warn("Failed to set value for characteristic {}: {}", channelUID.getId(), e.getMessage());
                }
            }
        }
    }

    private <T> void handleCharacteristicCommand(Characteristic<T> characteristic, Command command) throws Exception {
        if (command instanceof State) {
            @SuppressWarnings("unchecked")
            T value = (T) command;
            characteristic.setValue(value);
        }
    }
}
