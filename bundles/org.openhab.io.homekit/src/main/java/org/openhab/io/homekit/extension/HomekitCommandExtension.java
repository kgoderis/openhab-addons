package org.openhab.io.homekit.extension;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.bridge.HomekitAccessoryBridge;
import org.openhab.io.homekit.bridge.HomekitItemBridge;
import org.openhab.io.homekit.bridge.HomekitThingBridge;
import org.openhab.io.homekit.config.HomekitConfigurationManager;
import org.openhab.io.homekit.config.HomekitConfigurationManager.ConfigurationType;
import org.openhab.io.homekit.core.server.HomekitAccessoryServerUIDImpl;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.util.HomekitUID;
import org.openhab.io.homekit.util.ItemUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Console commands for interacting with the Homekit integration
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
@Component(service = ConsoleCommandExtension.class)
public class HomekitCommandExtension extends AbstractConsoleCommandExtension {
    private static final String COMMAND_HOMEKIT = "homekit";
    private static final String SUBCOMMAND_SHOW = "show";
    private static final String SUBCOMMAND_LIST = "list";
    private static final String SUBCOMMAND_PRINT = "print";
    private static final String SUBCOMMAND_HELP = "help";
    private static final String SUBCOMMAND_ITEM = "item";
    private static final String SUBCOMMAND_THING = "thing";
    private static final String SUBCOMMAND_SERVER = "server";
    private static final String SUBCOMMAND_SERVERS = "servers";
    private static final String SUBCOMMAND_ACCESSORIES = "accessories";
    private static final String SUBCOMMAND_BRIDGED_ACCESSORIES = "bridged-accessories";
    private static final String SUBCOMMAND_PAIRINGS = "pairings";
    private static final String SUBCOMMAND_ADD_PAIRING = "add-pairing";
    private static final String SUBCOMMAND_REMOVE_PAIRING = "remove-pairing";

    private final Logger logger = LoggerFactory.getLogger(HomekitCommandExtension.class);

    private final ItemRegistry itemRegistry;
    private final ThingRegistry thingRegistry;
    private final MetadataRegistry metadataRegistry;
    private final HomekitAccessoryRegistry accessoryRegistry;
    private final HomekitAccessoryServerRegistry accessoryServerRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final HomekitConfigurationManager configManager;
    private final HomekitItemBridge itemBridge;
    private final HomekitThingBridge thingBridge;
    private final HomekitAccessoryBridge accessoryBridge;

    @Activate
    public HomekitCommandExtension(@Reference ItemRegistry itemRegistry, @Reference ThingRegistry thingRegistry,
            @Reference MetadataRegistry metadataRegistry, @Reference HomekitAccessoryRegistry accessoryRegistry,
            @Reference HomekitAccessoryServerRegistry accessoryServerRegistry,
            @Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            @Reference HomekitConfigurationManager configManager, @Reference HomekitItemBridge itemBridge,
            @Reference HomekitThingBridge thingBridge, @Reference HomekitAccessoryBridge accessoryBridge) {
        super(COMMAND_HOMEKIT, "HomeKit integration commands");
        this.itemRegistry = itemRegistry;
        this.thingRegistry = thingRegistry;
        this.metadataRegistry = metadataRegistry;
        this.accessoryRegistry = accessoryRegistry;
        this.accessoryServerRegistry = accessoryServerRegistry;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.configManager = configManager;
        this.itemBridge = itemBridge;
        this.thingBridge = thingBridge;
        this.accessoryBridge = accessoryBridge;
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCOMMAND_SHOW:
                    if (args.length > 2) {
                        String type = args[1];
                        String id = args[2];
                        switch (type) {
                            case SUBCOMMAND_ITEM:
                                showItemMapping(id, console);
                                break;
                            case SUBCOMMAND_THING:
                                showThingMapping(id, console);
                                break;
                            case SUBCOMMAND_SERVER:
                                showServer(id, console);
                                break;
                            default:
                                console.println("Unknown type: " + type);
                                printUsage(console);
                        }
                    } else {
                        console.println("Missing type or id");
                        printUsage(console);
                    }
                    break;
                case SUBCOMMAND_LIST:
                    if (args.length > 1) {
                        String type = args[1];
                        switch (type) {
                            case SUBCOMMAND_SERVERS:
                                printServers(console);
                                break;
                            case SUBCOMMAND_BRIDGED_ACCESSORIES:
                                printBridgedAccessories(console);
                                break;
                            case SUBCOMMAND_ACCESSORIES:
                                printAccessories(console);
                                break;
                            case SUBCOMMAND_PAIRINGS:
                                printPairings(console);
                                break;
                            default:
                                console.println("Unknown type: " + type);
                                printUsage(console);
                        }
                    } else {
                        console.println("Missing type");
                        printUsage(console);
                    }
                    break;
                case SUBCOMMAND_ADD_PAIRING:
                    if (args.length > 2) {
                        String serverId = args[1];
                        String setupCode = args[2];
                        addPairing(serverId, setupCode, console);
                    } else {
                        console.println("Missing server ID or setup code");
                        printUsage(console);
                    }
                    break;
                case SUBCOMMAND_REMOVE_PAIRING:
                    if (args.length > 2) {
                        String serverId = args[1];
                        String pairingId = args[2];
                        removePairing(serverId, pairingId, console);
                    } else {
                        console.println("Missing server ID or pairing ID");
                        printUsage(console);
                    }
                    break;
                case SUBCOMMAND_HELP:
                    printUsage(console);
                    break;
                default:
                    console.println("Unknown command: " + subCommand);
                    printUsage(console);
            }
        } else {
            printUsage(console);
        }
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(
                buildCommandUsage(SUBCOMMAND_SHOW + " " + SUBCOMMAND_ITEM + " <itemName>",
                        "Show HomeKit mapping for an item"),
                buildCommandUsage(SUBCOMMAND_SHOW + " " + SUBCOMMAND_THING + " <thingId>",
                        "Show HomeKit mapping for a thing"),
                buildCommandUsage(SUBCOMMAND_SHOW + " " + SUBCOMMAND_SERVER + " <serverId>",
                        "Show HomeKit accessories exposed by an accessory server"),
                buildCommandUsage(SUBCOMMAND_LIST + " " + SUBCOMMAND_SERVERS, "List all HomeKit accessory servers"),
                buildCommandUsage(SUBCOMMAND_LIST + " " + SUBCOMMAND_ACCESSORIES, "List all HomeKit accessories"),
                buildCommandUsage(SUBCOMMAND_LIST + " " + SUBCOMMAND_BRIDGED_ACCESSORIES,
                        "List all HomeKit accessories exposed by the HomeKit bridges"),
                buildCommandUsage(SUBCOMMAND_LIST + " " + SUBCOMMAND_PAIRINGS, "List all HomeKit pairings"),
                buildCommandUsage(SUBCOMMAND_ADD_PAIRING + " <serverId> <setupCode>", "Add a pairing to a server"),
                buildCommandUsage(SUBCOMMAND_REMOVE_PAIRING + " <serverId> <pairingId>",
                        "Remove a pairing from a server"),
                buildCommandUsage(SUBCOMMAND_HELP, "Show this help"));
    }

    private void showItemMapping(String itemName, Console console) {
        Item item = itemRegistry.get(itemName);
        if (item == null) {
            console.println("Item not found: " + itemName);
            return;
        }

        console.println("Item: " + itemName);
        console.println("Type: " + item.getType());
        console.println("Label: " + item.getLabel());

        // Get HomeKit tags
        MetadataKey key = new MetadataKey("homekit", itemName);
        Metadata metadata = metadataRegistry.get(key);
        if (metadata != null) {
            console.println("HomeKit Tags: " + metadata.getValue());
        }

        // Get configuration
        Optional<Map<String, Object>> config = configManager.getConfiguration(new ItemUID(itemName),
                ConfigurationType.ITEM);
        if (config.isPresent()) {
            console.println("Configuration:");
            config.get().forEach((k, v) -> console.println("  " + k + ": " + v));
        }

        // Get group membership
        if (item instanceof GroupItem) {
            GroupItem groupItem = (GroupItem) item;
            console.println("Group Members:");
            groupItem.getMembers().forEach(member -> console.println("  " + member.getName()));
        }

        // Get HomeKit accessory mapping
        itemBridge.getMappedAccessory(itemName).ifPresent(accessory -> {
            console.println("HomeKit Accessory:");
            console.println("  UID: " + accessory.getUID());
            console.println("  Type: " + accessory.getClass().getSimpleName());
            console.println("  Services:");
            accessory.getServices().forEach(service -> {
                console.println("    " + service.getType());
                console.println("      Characteristics:");
                service.getCharacteristics().forEach(characteristic -> {
                    console.println("        " + characteristic.getType());
                    console.println("          Tag: " + characteristic.getTag());
                    console.println("          Value: " + characteristic.getValue());
                });
            });
        });

        // Get linked channels
        console.println("Linked Channels:");
        itemChannelLinkRegistry.getLinks(itemName).forEach(link -> {
            console.println("  " + link.getUID());
            Optional<Map<String, Object>> linkConfig = configManager.getConfiguration(new ChannelUID(link.getUID()),
                    ConfigurationType.CHANNEL);
            if (linkConfig.isPresent()) {
                console.println("    Configuration:");
                linkConfig.get().forEach((k, v) -> console.println("      " + k + ": " + v));
            }
        });
    }

    private void showThingMapping(String thingId, Console console) {
        Thing thing = thingRegistry.get(new ThingUID(thingId));
        if (thing == null) {
            console.println("Thing not found: " + thingId);
            return;
        }

        console.println("Thing: " + thingId);
        console.println("Type: " + thing.getThingTypeUID());
        console.println("Label: " + thing.getLabel());

        // Get configuration
        Optional<Map<String, Object>> config = configManager.getConfiguration(thing.getUID(), ConfigurationType.THING);
        if (config.isPresent()) {
            console.println("Configuration:");
            config.get().forEach((k, v) -> console.println("  " + k + ": " + v));
        }

        // Get HomeKit accessory mapping
        thingBridge.getMappedAccessory(thing.getUID()).ifPresent(accessory -> {
            console.println("HomeKit Accessory:");
            console.println("  UID: " + accessory.getUID());
            console.println("  Type: " + accessory.getClass().getSimpleName());
            console.println("  Services:");
            accessory.getServices().forEach(service -> {
                console.println("    " + service.getType());
                console.println("      Characteristics:");
                service.getCharacteristics().forEach(characteristic -> {
                    console.println("        " + characteristic.getType());
                    console.println("          Tag: " + characteristic.getTag());
                    console.println("          Value: " + characteristic.getValue());
                });
            });
        });

        // Get channels
        console.println("Channels:");
        thing.getChannels().forEach(channel -> {
            console.println("  " + channel.getUID());
            Optional<Map<String, Object>> channelConfig = configManager.getConfiguration(channel.getUID(),
                    ConfigurationType.CHANNEL);
            if (channelConfig.isPresent()) {
                console.println("    Configuration:");
                channelConfig.get().forEach((k, v) -> console.println("      " + k + ": " + v));
            }

            // Get linked items
            console.println("    Linked Items:");
            itemChannelLinkRegistry.getLinks(channel.getUID()).forEach(link -> {
                console.println("      " + link.getItemName());
                Optional<Map<String, Object>> linkConfig = configManager.getConfiguration(new ChannelUID(link.getUID()),
                        ConfigurationType.CHANNEL);
                if (linkConfig.isPresent()) {
                    console.println("        Configuration:");
                    linkConfig.get().forEach((k, v) -> console.println("          " + k + ": " + v));
                }
            });
        });
    }

    private void showServer(String serverId, Console console) {
        HomekitAccessoryServer server = accessoryServerRegistry.get(new HomekitAccessoryServerUIDImpl(serverId));
        if (server == null) {
            console.println("Server not found: " + serverId);
            return;
        }

        console.println("Accessory Server: " + serverId);
        console.println("Type: " + server.getClass().getSimpleName());
        console.println("Port: " + server.getPort());
        console.println("Setup Code: " + server.getSetupCode());
        console.println("State: " + (server.isPaired() ? "Paired" : "Unpaired"));

        // Get configuration
        Optional<Map<String, Object>> config = configManager
                .getConfiguration(new HomekitUID("homekit:server:" + serverId), ConfigurationType.BRIDGE);
        if (config.isPresent()) {
            console.println("Configuration:");
            config.get().forEach((k, v) -> console.println("  " + k + ": " + v));
        }

        // Get accessories
        console.println("Accessories:");
        try {
            for (HomekitAccessory accessory : server.getAccessories()) {
                console.println("  " + accessory.getUID());
                console.println("    Type: " + accessory.getClass().getSimpleName());
                console.println("    Services:");
                accessory.getServices().forEach(service -> {
                    console.println("      " + service.getType());
                    console.println("        Characteristics:");
                    service.getCharacteristics().forEach(characteristic -> {
                        console.println("          " + characteristic.getType());
                    });
                });
            }
        } catch (HomekitAccessoryOperationException e) {
            console.println("Error accessing accessories: " + e.getMessage());
        }
    }

    private void printServers(Console console) {
        console.println("HomeKit Accessory Servers:");
        accessoryServerRegistry.getAll().forEach(server -> {
            console.println("  " + server.getUID());
            console.println("    Type: " + server.getClass().getSimpleName());
            console.println("    Port: " + server.getPort());
            console.println("    Setup Code: " + server.getSetupCode());
            console.println("    State: " + (server.isPaired() ? "Paired" : "Unpaired"));
            try {
                console.println("    Accessories: " + server.getAccessories().size());
            } catch (HomekitAccessoryOperationException e) {
                console.println("    Error accessing accessories: " + e.getMessage());
            }
        });
    }

    private void printBridgedAccessories(Console console) {
        console.println("Bridged HomeKit Accessories:");
        console.println("----------------------------");

        // Print accessories from item bridge
        console.println("\nItem Bridge Accessories:");
        itemBridge.getItems().forEach(item -> {
            itemBridge.getMappedAccessory(item.getName()).ifPresent(accessory -> {
                printAccessoryDetails(console, accessory);
            });
        });

        // Print accessories from thing bridge
        console.println("\nThing Bridge Accessories:");
        thingBridge.getThings().forEach(thing -> {
            thingBridge.getMappedAccessory(thing.getUID()).ifPresent(accessory -> {
                printAccessoryDetails(console, accessory);
            });
        });

        // Print accessories from the accessory bridge
        console.println("\nAccessory Bridge Accessories:");
        accessoryBridge.getAccessories().forEach(accessory -> {
            printAccessoryDetails(console, accessory);
        });
    }

    private void printAccessories(Console console) {
        console.println("HomeKit Accessories:");
        console.println("--------------------");
        accessoryRegistry.getAll().forEach(accessory -> {
            printAccessoryDetails(console, accessory);
        });
    }

    private void printPairings(Console console) {
        console.println("HomeKit Pairings:");
        accessoryServerRegistry.getAll().forEach(server -> {
            console.println("  Server: " + server.getUID());
            try {
                server.getPairings().forEach(pairing -> {
                    console.println(
                            "    Pairing ID: " + Base64.getEncoder().encodeToString(pairing.getDestinationId()));
                    console.println("      Public Key: " + Base64.getEncoder().encodeToString(pairing.getPublicKey()));
                });
            } catch (HomekitServerException e) {
                console.println("Error getting pairings from server: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void addPairing(String serverId, String setupCode, Console console) {
        HomekitAccessoryServer server = accessoryServerRegistry.get(new HomekitAccessoryServerUIDImpl(serverId));
        if (server == null) {
            console.println("Server not found: " + serverId);
            return;
        }

        server.setSetupCode(setupCode);
        try {
            server.pairSetup();
        } catch (HomekitServerException e) {
            console.println("Error adding pairing to server: " + e.getMessage());
            e.printStackTrace();
        }
        console.println("Successfully added pairing to server: " + serverId);
    }

    private void removePairing(String serverId, String pairingId, Console console) {
        HomekitAccessoryServer server = accessoryServerRegistry.get(new HomekitAccessoryServerUIDImpl(serverId));
        if (server == null) {
            console.println("Server not found: " + serverId);
            return;
        }

        try {
            server.removePairing(Base64.getDecoder().decode(pairingId));
        } catch (HomekitServerException e) {
            console.println("Error removing pairing from server: " + e.getMessage());
            e.printStackTrace();
        }
        console.println("Successfully removed pairing from server: " + serverId);
    }

    private void printAccessoryDetails(Console console, HomekitAccessory accessory) {
        console.println("Accessory Details:");
        console.println("-----------------");
        console.println("Label: " + accessory.getLabel());
        console.println("ID: " + accessory.getAccessoryId());
        console.println("Manufacturer: " + accessory.getManufacturer());
        console.println("Model: " + accessory.getModel());
        console.println("Serial Number: " + accessory.getSerialNumber());
        console.println("\nServices:");
        for (HomekitService service : accessory.getServices()) {
            console.println("  " + service.getType() + " (ID: " + service.getInstanceId() + ")");
            console.println("    Characteristics:");
            for (HomekitCharacteristic<?> characteristic : service.getCharacteristics()) {
                console.println("      " + characteristic.getType());
            }
        }
    }
}
