package org.openhab.io.homekit;

import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerRegistry;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.Service;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
public class HomekitItemChannelLinkRegistryChangeListener implements RegistryChangeListener<ItemChannelLink> {

    @Reference
    private ItemRegistry itemRegistry;
    @Reference
    private AccessoryServerRegistry accessoryServerRegistry;

    private ItemChannelLinkRegistry linkRegistry;

    @Reference
    protected void setItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        linkRegistry = itemChannelLinkRegistry;
        itemChannelLinkRegistry.addRegistryChangeListener(this);
    }

    protected void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        linkRegistry = null;
    }

    @Override
    public void added(ItemChannelLink element) {
        for (AccessoryServer server : accessoryServerRegistry.getAll()) {
            for (Accessory accessory : server.getAccessories()) {
                for (Service service : accessory.getServices()) {
                    for (@SuppressWarnings("rawtypes")
                    Characteristic characteristic : service.getCharacteristics()) {
                        if (characteristic.getChannelUID() == element.getLinkedUID()) {
                            GenericItem item = (GenericItem) itemRegistry.get(element.getItemName());
                            if (item != null) {
                                item.addStateChangeListener(characteristic);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void removed(ItemChannelLink element) {
        for (AccessoryServer server : accessoryServerRegistry.getAll()) {
            for (Accessory accessory : server.getAccessories()) {
                for (Service service : accessory.getServices()) {
                    for (@SuppressWarnings("rawtypes")
                    Characteristic characteristic : service.getCharacteristics()) {
                        if (characteristic.getChannelUID() == element.getLinkedUID()) {
                            GenericItem item = (GenericItem) itemRegistry.get(element.getItemName());
                            if (item != null) {
                                item.removeStateChangeListener(characteristic);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void updated(ItemChannelLink oldElement, ItemChannelLink element) {
        removed(oldElement);
        added(element);
    }

}
