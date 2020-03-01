package org.openhab.io.homekit.internal.client;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.Characteristic;

public class HomekitBridgeDiscoveryService extends AbstractDiscoveryService implements HomekitStatusListener {

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.unmodifiableSet(Stream
            .of(HomekitAccessoryHandler.SUPPORTED_THING_TYPES.stream(),
                    HomekitBridgeAccessoryHandler.SUPPORTED_THING_TYPES.stream())
            .flatMap(i -> i).collect(Collectors.toSet()));

    private static final int SEARCH_TIME = 10;

    private final HomekitBridgeHandler homekitBridgeHandler;

    public HomekitBridgeDiscoveryService(HomekitBridgeHandler homekitBridgeHandler) {
        super(SEARCH_TIME);
        this.homekitBridgeHandler = homekitBridgeHandler;
    }

    @Override
    protected void startScan() {
        homekitBridgeHandler.startSearch();
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

    public void activate() {
        homekitBridgeHandler.registerHomekitStatusListener(this);
    }

    @Override
    public void deactivate() {
        removeOlderResults(new Date().getTime(), homekitBridgeHandler.getThing().getUID());
        homekitBridgeHandler.unregisterHomekitStatusListener(this);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES;
    }

    @Override
    public void onAccessoryRemoved(@Nullable Bridge bridge, @NonNull Accessory accessory) {
        if (accessory.getId() == 1) {
            String id = bridge.getBridgeUID().getId().replace(":", "");
            ThingUID uid = new ThingUID(HomekitBindingConstants.THING_TYPE_BRIDGE, id);
            thingRemoved(uid);
        } else {
            ThingUID uid = new ThingUID(HomekitBindingConstants.THING_TYPE_ACCESSORY, bridge.getBridgeUID(),
                    String.valueOf(accessory.getId()));
            thingRemoved(uid);
        }
    }

    @Override
    public void onAccessoryAdded(@Nullable Bridge bridge, @NonNull Accessory accessory) {
        if (accessory.getId() == 1) {
            String id = bridge.getBridgeUID().getId().replace(":", "");
            ThingUID uid = new ThingUID(HomekitBindingConstants.THING_TYPE_BRIDGE, id);

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(uid)
                    .withThingType(HomekitBindingConstants.THING_TYPE_BRIDGE).withRepresentationProperty(uid.getId())
                    .withLabel("Homekit Accessory Bridge").build();

            thingDiscovered(discoveryResult);
        } else {
            ThingUID uid = new ThingUID(HomekitBindingConstants.THING_TYPE_ACCESSORY, bridge.getBridgeUID(),
                    String.valueOf(accessory.getId()));

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(uid)
                    .withThingType(HomekitBindingConstants.THING_TYPE_BRIDGE).withRepresentationProperty(uid.getId())
                    .withLabel("Homekit Accessory").build();

            thingDiscovered(discoveryResult);
        }
    }

    @Override
    public void onCharacteristicRemoved(@Nullable Bridge bridge, @NonNull Characteristic characteristic) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCharacteristicAdded(@Nullable Bridge bridge, @NonNull Characteristic characteristic) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCharacteristicStateChanged(@Nullable Bridge bridge, @NonNull Characteristic characteristic) {
        // TODO Auto-generated method stub

    }

}
