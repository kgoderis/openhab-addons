/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.io.homekit.api.factory;

import java.util.HashSet;
import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;

/**
 * The {@link HomekitFactory} is responsible for creating {@link Accessory}s based on {@link Thing}s. Therefore
 * the
 * factory must be registered as OSGi service.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public interface HomekitFactory {

    /**
     * Returns whether the factory is able to create an accessory for the given type.
     *
     * @param thingTypeUID the thing type UID
     * @return true, if the handler supports the thing type, false otherwise
     */
    boolean supportsThingType(ThingTypeUID thingTypeUID);

    boolean supportsServiceType(String serviceType);

    boolean supportsCharacteristicsType(String type);

    // /**
    //  * Creates a new {@link Accessory} instance.
    //  * <p>
    //  * This method is only called if the {@link HomekitFactory} supports the type of the given thing.
    //  * <p>
    //  *
    //  * @param thing the thing for which a new handler must be registered
    //  * @return the created thing handler instance, not null
    //  * @throws Exception
    //  * @throws IllegalStateException if the handler instance could not be created
    //  */
    // @Nullable
    // Accessory createAccessory(Thing thing, LocalAccessoryServer server) throws Exception;

    @Nullable
    Accessory createAccessory(Class<? extends Accessory> accessoryClass, AccessoryServer server,
            long instanceId, boolean extend);

    @Nullable
    Accessory createAccessory(Class<? extends Accessory> accessoryClass, AccessoryServer server,
            long instanceId);

    @Nullable
    Service createService(String serviceType, Accessory accessory, boolean extend, String serviceName);

    @Nullable
    Service createService(String serviceType, Accessory accessory, long instanceId, boolean extend,
            String serviceName);

    @Nullable
    Service createService(String serviceType, Accessory accessory, long instanceId, boolean extend);

    @Nullable Service createService( Accessory accessory, JsonValue value);


    @Nullable
    Characteristic<?> createCharacteristic(String characteristicsType, Service service);

    @Nullable
    Characteristic<?> createCharacteristic(String characteristicsType, Service service, long instanceId);

    /**
     * Creates a characteristic instance from a JSON value.
     * 
     * @param service The service to create the characteristic for
     * @param value The JSON value containing characteristic data
     * @return The created characteristic instance, or null if creation fails
     */
    @Nullable Characteristic<?> createCharacteristic( Service service, JsonValue value);

    void addAccessory(ThingTypeUID type, Class<? extends Accessory> accessoryClass);

    void addService(ThingTypeUID type);

    void addService(ThingTypeUID type, String serviceType);

    void addService(ThingTypeUID type, Class<? extends Service> serviceClass);

    void addService(String serviceType, Class<? extends Service> serviceClass);

    void addService(Class<@NonNull ? extends Service> serviceClass);

    void addServiceWithTag(String tag, Class<? extends Service> serviceClass);

    void addCharacteristic(ChannelTypeUID type, String characateristicType);

    void addCharacteristic(ChannelTypeUID type, Class<@NonNull ? extends Characteristic<?>> characteristicClass);

    void addCharacteristic(String characateristicType, Class<? extends Characteristic<?>> characteristicClass);

    void addCharacteristic(Class<@NonNull ? extends Characteristic<?>> characteristicClass);

    void addCharacteristicWithTag(String tag, Class<? extends Characteristic<?>> characteristicClass);

    HashSet<String> getCharacteristicTypes(@Nullable ChannelTypeUID type);

    ThingTypeUID[] getSupportedThingTypes();

    Set<String> getSupportedServiceTypes();

    boolean isServiceSupported(String serviceType);

    Set<String> getSupportedCharacteristicTypes();

    boolean isCharacteristicSupported(String characteristicType);

    @Nullable
    String getCharacteristicAcceptedItemType(String characteristicType);

    @Nullable
    ChannelTypeUID getChannelTypeUID(String characteristicType);

    @Nullable
    Class<? extends Service> getService(String characteristicType);

    @Nullable
    Class<? extends Characteristic<?>> getCharacteristic(String serviceType);

    @Nullable
    String getServiceTypeFromTag(String firstTag);

    @Nullable
    String getCharacteristicTypeFromTag(String firstTag);

    @Nullable
    String getTagFromServiceType(String serviceType);

}
