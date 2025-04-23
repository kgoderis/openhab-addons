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
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.openhab.io.homekit.exception.MetadataException;
import org.openhab.io.homekit.exception.RegistrationException;

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

    ThingTypeUID[] getSupportedThingTypes();

    boolean supportsServiceType(String serviceType);

    Set<String> getSupportedServiceTypes();

    boolean supportsCharacteristicsType(String characteristicType);

    Set<String> getSupportedCharacteristicTypes();

    @Nullable
    Accessory createAccessory(Class<? extends Accessory> accessoryClass, AccessoryServer server, long instanceId,
            boolean extend) throws RegistrationException;

    @Nullable
    Accessory createAccessory(Class<? extends Accessory> accessoryClass, AccessoryServer server, long instanceId) throws RegistrationException;

    @Nullable
    Service createService(String serviceType, Accessory accessory, boolean extend, String serviceName) throws MetadataException;

    @Nullable
    Service createService(String serviceType, Accessory accessory, long instanceId, boolean extend, String serviceName) throws MetadataException;

    @Nullable
    Service createService(String serviceType, Accessory accessory, long instanceId, boolean extend) throws MetadataException;

    @Nullable
    Service createService(Accessory accessory, JsonValue value) throws MetadataException;

    @Nullable
    Characteristic<?> createCharacteristic(String characteristicsType, Service service) throws MetadataException;

    @Nullable
    Characteristic<?> createCharacteristic(String characteristicsType, Service service, long instanceId) throws MetadataException;

    @Nullable
    Characteristic<?> createCharacteristic(Service service, JsonValue value) throws MetadataException;

    void addAccessory(ThingTypeUID thingTypeUID, Class<? extends Accessory> accessoryClass) throws RegistrationException;

    void addService(ThingTypeUID thingTypeUID) throws RegistrationException;

    void addService(ThingTypeUID thingTypeUID, String serviceType) throws RegistrationException;

    void addService(ThingTypeUID thingTypeUID, Class<? extends Service> serviceClass) throws RegistrationException;

    void addService(String serviceType, Class<? extends Service> serviceClass) throws RegistrationException;

    void addService(Class<@NonNull ? extends Service> serviceClass) throws RegistrationException;

    void addCharacteristic(ChannelTypeUID channelTypeUID, String characteristicType) throws RegistrationException;

    void addCharacteristic(ChannelTypeUID channelTypeUID, Class<@NonNull ? extends Characteristic<?>> characteristicClass) throws RegistrationException;

    void addCharacteristic(String characteristicType, Class<? extends Characteristic<?>> characteristicClass) throws RegistrationException;

    void addCharacteristic(Class<@NonNull ? extends Characteristic<?>> characteristicClass) throws RegistrationException;

    HashSet<String> getCharacteristicTypes(@Nullable ChannelTypeUID channelTypeUID);

    @Nullable
    String getCharacteristicAcceptedItemType(String characteristicType);

    @Nullable
    ChannelTypeUID getChannelTypeUID(String characteristicType);

    @Nullable
    Class<? extends Service> getService(String serviceType);

    @Nullable
    Class<? extends Characteristic<?>> getCharacteristic(String characteristicType);

    @Nullable
    String getServiceTypeFromTag(String tag);

    @Nullable
    String getCharacteristicTypeFromTag(String tag);

    @Nullable
    String getTagFromServiceType(String serviceType);

    @Nullable
    String getTagFromCharacteristicType(String characteristicType);

    @Nullable
    String getServiceType(String characteristicType);
}
