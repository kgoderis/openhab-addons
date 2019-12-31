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
package org.openhab.io.homekit.api;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link HomekitFactory} is responsible for creating {@link Accessory}s based on {@link Thing}s. Therefore the
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

    /**
     * Creates a new {@link Accessory} instance.
     * <p>
     * This method is only called if the {@link HomekitFactory} supports the type of the given thing.
     * <p>
     *
     * @param thing the thing for which a new handler must be registered
     * @return the created thing handler instance, not null
     * @throws Exception
     * @throws IllegalStateException if the handler instance could not be created
     */
    @Nullable
    Accessory createAccessory(Thing thing, AccessoryServer server) throws Exception;

    @Nullable
    Accessory createAccessory(Class<? extends Accessory> accessoryClass, AccessoryServer server, long instanceId,
            boolean extend);

    @Nullable
    Service createService(String serviceType, Accessory accessory, boolean extend, String serviceName);

    @Nullable
    Service createService(String serviceType, Accessory accessory, long instanceId, boolean extend, String serviceName);

    @Nullable
    Service createService(String serviceType, Accessory accessory, long instanceId, boolean extend);

    @Nullable
    Characteristic<?> createCharacteristic(String characteristicsType, Service service);

    @Nullable
    Characteristic<?> createCharacteristic(String characteristicsType, Service service, long instanceId);

    void addAccessory(ThingTypeUID type, Class<? extends Accessory> accessoryClass);

    void addService(ThingTypeUID type);

    void addService(ThingTypeUID type, String serviceType);

    void addService(ThingTypeUID type, Class<? extends Service> serviceClass);

    void addService(String serviceType, Class<? extends Service> serviceClass);

    void addService(Class<@NonNull ? extends Service> serviceClass);

    void addCharacteristic(ChannelTypeUID type, String characateristicType);

    void addCharacteristic(ChannelTypeUID type, Class<@NonNull ? extends Characteristic<?>> characteristicClass);

    void addCharacteristic(String characateristicType, Class<? extends Characteristic<?>> characteristicClass);

    void addCharacteristic(Class<@NonNull ? extends Characteristic<?>> characteristicClass);

    HashSet<String> getCharacteristicTypes(@Nullable ChannelTypeUID type);

    ThingTypeUID[] getSupportedThingTypes();

    Set<String> getSupportedServiceTypes();

    Set<String> getSupportedCharacteristicTypes();

}
