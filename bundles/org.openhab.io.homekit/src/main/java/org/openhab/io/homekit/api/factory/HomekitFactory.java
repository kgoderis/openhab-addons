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

import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.openhab.io.homekit.exception.HomekitRegistrationException;

/**
 * The {@link HomekitFactory} is responsible for creating {@link HomekitAccessory}s based on {@link Thing}s. Therefore
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

    ThingTypeUID[] getSupportedThingTypes();

    boolean supportsAccessoryClass(Class<? extends HomekitAccessory> accessoryClass);

    Set<Class<? extends HomekitAccessory>> getSupportedAccessoryClasses();

    boolean supportsServiceType(String serviceType);

    Set<String> getSupportedServiceTypes();

    boolean supportsCharacteristicsType(String characteristicType);

    Set<String> getSupportedCharacteristicTypes();

    @Nullable
    HomekitAccessory createAccessory(Class<? extends HomekitAccessory> accessoryClass, HomekitAccessoryServer server, long instanceId,
            boolean extend) throws HomekitFactoryException;

    @Nullable
    HomekitAccessory createAccessory(Class<? extends HomekitAccessory> accessoryClass, HomekitAccessoryServer server, long instanceId)
            throws HomekitFactoryException;

    @Nullable
    HomekitAccessory createAccessory(Thing thing, HomekitAccessoryServer server) throws HomekitFactoryException;

    public @Nullable HomekitAccessory createAccessory(Class<? extends HomekitAccessory> accessoryClass, JsonValue value)
            throws HomekitFactoryException;

    @Nullable
    HomekitService createService(String serviceType, HomekitAccessory accessory, boolean extend, String serviceName)
            throws HomekitFactoryException;

    @Nullable
    HomekitService createService(String serviceType, HomekitAccessory accessory, long instanceId, boolean extend, String serviceName)
            throws HomekitFactoryException;

    @Nullable
    HomekitService createService(String serviceType, HomekitAccessory accessory, long instanceId, boolean extend)
            throws HomekitFactoryException;

    @Nullable
    HomekitService createService(HomekitAccessory accessory, JsonValue value) throws HomekitFactoryException;

    @Nullable
    HomekitCharacteristic<?> createCharacteristic(String characteristicsType, HomekitService service) throws HomekitFactoryException;

    @Nullable
    HomekitCharacteristic<?> createCharacteristic(String characteristicsType, HomekitService service, long instanceId)
            throws HomekitFactoryException;

    @Nullable
    HomekitCharacteristic<?> createCharacteristic(HomekitService service, JsonValue value) throws HomekitFactoryException;

    void addAccessory(ThingTypeUID thingTypeUID, Class<? extends HomekitAccessory> accessoryClass)
            throws HomekitRegistrationException;

    void addAccessory(Class<? extends HomekitAccessory> accessoryClass) throws HomekitRegistrationException;

    void addService(ThingTypeUID thingTypeUID) throws HomekitRegistrationException;

    void addService(ThingTypeUID thingTypeUID, String serviceType) throws HomekitRegistrationException;

    void addService(ThingTypeUID thingTypeUID, Class<? extends HomekitService> serviceClass)
            throws HomekitRegistrationException;

    void addService(String serviceType, Class<? extends HomekitService> serviceClass) throws HomekitRegistrationException;

    void addService(Class<@NonNull ? extends HomekitService> serviceClass) throws HomekitRegistrationException;

    void addServiceWithTag(String tag, Class<? extends HomekitService> serviceClass) throws HomekitRegistrationException;

    void addCharacteristic(ChannelTypeUID channelTypeUID, String characteristicType)
            throws HomekitRegistrationException;

    void addCharacteristic(ChannelTypeUID channelTypeUID,
            Class<@NonNull ? extends HomekitCharacteristic<?>> characteristicClass) throws HomekitRegistrationException;

    void addCharacteristic(String characteristicType, Class<? extends HomekitCharacteristic<?>> characteristicClass)
            throws HomekitRegistrationException;

    void addCharacteristic(Class<@NonNull ? extends HomekitCharacteristic<?>> characteristicClass)
            throws HomekitRegistrationException;

    void addCharacteristicWithTag(String tag, Class<? extends HomekitCharacteristic<?>> characteristicClass)
            throws HomekitRegistrationException;

    @Nullable
    Set<String> getCharacteristicTypes(ChannelTypeUID channelTypeUID);

    @Nullable
    String getCharacteristicAcceptedItemType(String characteristicType);

    @Nullable
    ChannelTypeUID getChannelTypeUID(String characteristicType);

    @Nullable
    Class<? extends HomekitService> getService(String serviceType);

    @Nullable
    Class<? extends HomekitCharacteristic<?>> getCharacteristic(String characteristicType);

    @Nullable
    String getServiceTypeFromTag(String tag);

    @Nullable
    String getCharacteristicTypeFromTag(String tag);

    @Nullable
    String getTagFromServiceType(String serviceType);

    @Nullable
    String getTagFromCharacteristicType(String characteristicType);
}
