package org.openhab.io.homekit.internal.handler;
// package org.openhab.io.homekit.internal.server;

// import java.util.Set;

// import org.eclipse.jdt.annotation.NonNullByDefault;
// import org.openhab.core.thing.ManagedThingProvider;
// import org.openhab.core.thing.Thing;
// import org.openhab.core.thing.ThingTypeUID;
// import org.openhab.core.thing.ThingUID;
// import org.openhab.core.thing.binding.builder.ThingBuilder;
// import org.openhab.io.homekit.api.hap.Accessory;
// import org.openhab.io.homekit.api.listener.AccessoryRegistryChangeListener;
// import org.openhab.io.homekit.internal.provider.HomekitChannelGroupTypeProvider;
// import org.openhab.io.homekit.internal.provider.HomekitChannelTypeProvider;
// import org.osgi.service.component.annotations.Activate;
// import org.osgi.service.component.annotations.Component;
// import org.osgi.service.component.annotations.Reference;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// @NonNullByDefault
// @Component(service = { AccessoryRegistryChangeListener.class })
// public class AccessoryServerThingFactory implements AccessoryRegistryChangeListener {

// private final Logger logger = LoggerFactory.getLogger(AccessoryServerThingFactory.class);
// private final ManagedThingProvider managedThingProvider;
// private final HomekitChannelTypeProvider channelTypeProvider;
// private final HomekitChannelGroupTypeProvider channelGroupTypeProvider;

// public static final String BINDING_ID = "homekit";
// public static final ThingTypeUID THING_TYPE_ACCESSORY_SERVER = new ThingTypeUID(BINDING_ID, "accessory-server");
// private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_ACCESSORY_SERVER);

// @Activate
// public AccessoryServerThingFactory(
// @Reference ManagedThingProvider managedThingProvider,
// @Reference HomekitChannelTypeProvider channelTypeProvider,
// @Reference HomekitChannelGroupTypeProvider channelGroupTypeProvider) {
// this.managedThingProvider = managedThingProvider;
// this.channelTypeProvider = channelTypeProvider;
// this.channelGroupTypeProvider = channelGroupTypeProvider;
// }

// public boolean supportsThingType(ThingTypeUID thingTypeUID) {
// return SUPPORTED_THING_TYPES.contains(thingTypeUID);
// }

// @Override
// public void added(Accessory accessory) {
// // Create a new Thing for the added Accessory
// ThingUID thingUID = new ThingUID(THING_TYPE_ACCESSORY_SERVER, accessory.getUID().toString());

// if (managedThingProvider.get(thingUID) == null) {
// Thing thing = ThingBuilder.create(THING_TYPE_ACCESSORY_SERVER, thingUID)
// .withLabel("HomeKit Accessory " + accessory.getUID().toString())
// .build();
// managedThingProvider.add(thing);
// logger.debug("Created new Thing for Accessory {}", accessory.getUID());
// }
// }

// @Override
// public void removed(Accessory accessory) {
// // Remove the Thing associated with the removed Accessory
// ThingUID thingUID = new ThingUID(THING_TYPE_ACCESSORY_SERVER, accessory.getUID().toString());
// Thing thing = managedThingProvider.get(thingUID);
// if (thing != null) {
// managedThingProvider.remove(thing.getUID());
// logger.debug("Removed Thing for Accessory {}", accessory.getUID());
// }
// }

// @Override
// public void updated(Accessory oldAccessory, Accessory newAccessory) {
// // Handle accessory updates by updating the Thing properties/configuration if needed
// ThingUID thingUID = new ThingUID(THING_TYPE_ACCESSORY_SERVER, newAccessory.getUID().toString());

// Thing thing = managedThingProvider.get(thingUID);
// if (thing != null) {
// managedThingProvider.remove(thing.getUID());
// logger.debug("Removed Thing for Accessory {}", accessory.getUID());

// thing = ThingBuilder.create(THING_TYPE_ACCESSORY_SERVER, thingUID)
// .withLabel("HomeKit Accessory " + newAccessory.getUID().toString())
// .build();
// managedThingProvider.update(thing);

// //TODO : Improve this to update the thing properties/configuration if needed
// }
// }
// }
