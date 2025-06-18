/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import org.openhab.io.homekit.bridge.HomekitItemBridge;
import org.openhab.io.homekit.bridge.HomekitPassthroughBridge;
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
 * Console commands for interacting with the HomeKit integration.
 * 
 * This class provides a set of console commands for managing and inspecting HomeKit integration
 * components, including servers, accessories, pairings, and their mappings to OpenHAB items and things.
 * 
 * Key features:
 * - View HomeKit mappings for items and things
 * - List and inspect HomeKit servers and accessories
 * - Manage HomeKit pairings
 * - Display detailed information about HomeKit components
 * 
 * The commands are accessible through the OpenHAB console using the 'homekit' command
 * followed by various subcommands.
 *
 * @author Karel Goderis - Initial contribution
 */
@Component(service = ConsoleCommandExtension.class)
@NonNullByDefault
public class HomekitCommandExtension extends AbstractConsoleCommandExtension {
    // ========== Command Constants ==========
    private static final String COMMAND_HOMEKIT = "homekit";
    private static final String SUBCOMMAND_SHOW = "show";
    private static final String SUBCOMMAND_LIST = "list";
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

    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit Command Extension: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_CMD = LOG_PREFIX + "Command - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    private static final String LOG_DEBUG = LOG_PREFIX + "Debug - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitCommandExtension.class);

    private final ItemRegistry itemRegistry;
    private final ThingRegistry thingRegistry;
    private final MetadataRegistry metadataRegistry;
    private final HomekitAccessoryRegistry accessoryRegistry;
    private final HomekitAccessoryServerRegistry accessoryServerRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final HomekitConfigurationManager configManager;
    private final HomekitItemBridge itemBridge;
    private final HomekitThingBridge thingBridge;
    private final HomekitPassthroughBridge accessoryBridge;

    @Activate
    public HomekitCommandExtension(@Reference ItemRegistry itemRegistry, @Reference ThingRegistry thingRegistry,
            @Reference MetadataRegistry metadataRegistry, @Reference HomekitAccessoryRegistry accessoryRegistry,
            @Reference HomekitAccessoryServerRegistry accessoryServerRegistry,
            @Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            @Reference HomekitConfigurationManager configManager, @Reference HomekitItemBridge itemBridge,
            @Reference HomekitThingBridge thingBridge, @Reference HomekitPassthroughBridge accessoryBridge) {
        super(COMMAND_HOMEKIT, "HomeKit integration commands");
        logger.info("{}Initializing HomeKit command extension", LOG_INIT);
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
        logger.debug("{}Command extension initialized with {} servers, {} accessories", LOG_INIT,
                accessoryServerRegistry.getAll().size(), accessoryRegistry.getAll().size());
    }

    /**
     * Executes a HomeKit console command.
     * 
     * This method processes the command arguments and executes the appropriate subcommand.
     * Available subcommands include:
     * - show: Display mappings for items, things, or servers
     * - list: List servers, accessories, or pairings
     * - add-pairing: Add a new pairing to a server
     * - remove-pairing: Remove a pairing from a server
     * - help: Display command usage
     *
     * @param args The command arguments, where args[0] is the subcommand
     * @param console The console to write output to
     */
    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            logger.debug("{}Executing command: {}", LOG_CMD, subCommand);
            switch (subCommand) {
                case SUBCOMMAND_SHOW:
                    if (args.length > 2) {
                        String type = args[1];
                        String id = args[2];
                        logger.debug("{}Showing mapping for {}: {}", LOG_CMD, type, id);
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
                                logger.warn("{}Unknown type: {}", LOG_WARN, type);
                                console.println("Unknown type: " + type);
                                printUsage(console);
                        }
                    } else {
                        logger.warn("{}Missing type or id for show command", LOG_WARN);
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
            logger.debug("{}No command arguments provided", LOG_CMD);
            printUsage(console);
        }
    }

    /**
     * Returns a list of command usage strings.
     * 
     * This method provides detailed usage information for each available command,
     * including the command syntax and a brief description of its purpose.
     *
     * @return A list of command usage strings
     */
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

    /**
     * Shows the HomeKit mapping for an item.
     * 
     * This method displays detailed information about an item's HomeKit configuration,
     * including its type, label, HomeKit tags, configuration, group membership,
     * and associated HomeKit accessory details.
     *
     * @param itemName The name of the item to show mapping for
     * @param console The console to write output to
     */
    private void showItemMapping(String itemName, Console console) {
        logger.debug("{}Showing mapping for item: {}", LOG_CMD, itemName);
        @SuppressWarnings("null") // itemRegistry.get() can return null, which is checked below
        Item item = itemRegistry.get(itemName);
        if (item == null) {
            logger.warn("{}Item not found: {}", LOG_WARN, itemName);
            console.println("Item not found: " + itemName);
            return;
        }

        logger.debug("{}Found item: {} (type: {}, label: {})", LOG_DEBUG, itemName, item.getType(), item.getLabel());
        console.println("Item: " + itemName);
        console.println("Type: " + item.getType());
        console.println("Label: " + item.getLabel());

        // Get HomeKit tags
        logger.debug("{}Retrieving HomeKit metadata for item: {}", LOG_DEBUG, itemName);
        MetadataKey key = new MetadataKey("homekit", itemName);
        @SuppressWarnings("null") // metadataRegistry.get() can return null, which is checked below
        Metadata metadata = metadataRegistry.get(key);
        if (metadata != null) {
            logger.debug("{}Found HomeKit metadata for item {}: {}", LOG_DEBUG, itemName, metadata.getValue());
            console.println("HomeKit Tags: " + metadata.getValue());
        } else {
            logger.debug("{}No HomeKit metadata found for item: {}", LOG_DEBUG, itemName);
        }

        // Get configuration
        logger.debug("{}Retrieving configuration for item: {}", LOG_DEBUG, itemName);
        Optional<Map<String, Object>> config = configManager.getConfiguration(new ItemUID(itemName),
                ConfigurationType.ITEM);
        if (config.isPresent()) {
            @SuppressWarnings("null") // Optional.get() is safe after isPresent() check
            Map<String, Object> configMap = config.get();
            logger.debug("{}Found configuration for item {}: {} entries", LOG_DEBUG, itemName, configMap.size());
            console.println("Configuration:");
            configMap.forEach((k, v) -> console.println("  " + k + ": " + v));
        } else {
            logger.debug("{}No configuration found for item: {}", LOG_DEBUG, itemName);
        }

        // Get group membership
        if (item instanceof GroupItem) {
            GroupItem groupItem = (GroupItem) item;
            logger.debug("{}Item {} is a group with {} members", LOG_DEBUG, itemName, groupItem.getMembers().size());
            console.println("Group Members:");
            groupItem.getMembers().forEach(member -> {
                logger.debug("{}Group member: {}", LOG_DEBUG, member.getName());
                console.println("  " + member.getName());
            });
        } else {
            logger.debug("{}Item {} is not a group item", LOG_DEBUG, itemName);
        }

        // Get HomeKit accessory mapping
        logger.debug("{}Looking for HomeKit accessory mapping for item: {}", LOG_DEBUG, itemName);
        itemBridge.getMappedAccessory(itemName).ifPresent(accessory -> {
            logger.debug("{}Found HomeKit accessory mapping for item {}: {}", LOG_CMD, itemName, accessory.getUID());
            console.println("HomeKit Accessory:");
            console.println("  UID: " + accessory.getUID());
            console.println("  Type: " + accessory.getClass().getSimpleName());
            console.println("  Services:");
            accessory.getServices().forEach(service -> {
                logger.debug("{}Service: {} (ID: {})", LOG_DEBUG, service.getType(), service.getInstanceId());
                console.println("    " + service.getType());
                console.println("      Characteristics:");
                service.getCharacteristics().forEach(characteristic -> {
                    logger.debug("{}Characteristic: {} (tag: {}, value: {})", LOG_DEBUG, characteristic.getType(),
                            characteristic.getTag(), characteristic.getValue());
                    console.println("        " + characteristic.getType());
                    console.println("          Tag: " + characteristic.getTag());
                    console.println("          Value: " + characteristic.getValue());
                });
            });
        });

        // Get linked channels
        logger.debug("{}Retrieving linked channels for item: {}", LOG_DEBUG, itemName);
        console.println("Linked Channels:");
        itemChannelLinkRegistry.getLinks(itemName).forEach(link -> {
            logger.debug("{}Found linked channel: {}", LOG_DEBUG, link.getUID());
            console.println("  " + link.getUID());
            Optional<Map<String, Object>> linkConfig = configManager.getConfiguration(new ChannelUID(link.getUID()),
                    ConfigurationType.CHANNEL);
            if (linkConfig.isPresent()) {
                @SuppressWarnings("null") // Optional.get() is safe after isPresent() check
                Map<String, Object> linkConfigMap = linkConfig.get();
                logger.debug("{}Found configuration for channel {}: {} entries", LOG_DEBUG, link.getUID(),
                        linkConfigMap.size());
                console.println("    Configuration:");
                linkConfigMap.forEach((k, v) -> console.println("      " + k + ": " + v));
            } else {
                logger.debug("{}No configuration found for channel: {}", LOG_DEBUG, link.getUID());
            }
        });
        logger.debug("{}Completed showing mapping for item: {}", LOG_CMD, itemName);
    }

    /**
     * Shows the HomeKit mapping for a thing.
     * 
     * This method displays detailed information about a thing's HomeKit configuration,
     * including its type, label, channels, and associated HomeKit accessory details.
     *
     * @param thingId The ID of the thing to show mapping for
     * @param console The console to write output to
     */
    private void showThingMapping(String thingId, Console console) {
        logger.debug("{}Showing mapping for thing: {}", LOG_CMD, thingId);
        @SuppressWarnings("null") // thingRegistry.get() can return null, which is checked below
        Thing thing = thingRegistry.get(new ThingUID(thingId));
        if (thing == null) {
            logger.warn("{}Thing not found: {}", LOG_WARN, thingId);
            console.println("Thing not found: " + thingId);
            return;
        }

        logger.debug("{}Found thing: {} (type: {}, label: {})", LOG_DEBUG, thingId, thing.getThingTypeUID(),
                thing.getLabel());
        console.println("Thing: " + thingId);
        console.println("Type: " + thing.getThingTypeUID());
        console.println("Label: " + thing.getLabel());

        // Get configuration
        logger.debug("{}Retrieving configuration for thing: {}", LOG_DEBUG, thingId);
        Optional<Map<String, Object>> config = configManager.getConfiguration(thing.getUID(), ConfigurationType.THING);
        if (config.isPresent()) {
            @SuppressWarnings("null") // Optional.get() is safe after isPresent() check
            Map<String, Object> configMap = config.get();
            logger.debug("{}Found configuration for thing {}: {} entries", LOG_DEBUG, thingId, configMap.size());
            console.println("Configuration:");
            configMap.forEach((k, v) -> console.println("  " + k + ": " + v));
        } else {
            logger.debug("{}No configuration found for thing: {}", LOG_DEBUG, thingId);
        }

        // Get HomeKit accessory mapping
        logger.debug("{}Looking for HomeKit accessory mapping for thing: {}", LOG_DEBUG, thingId);
        thingBridge.getMappedAccessory(thing.getUID()).ifPresent(accessory -> {
            logger.debug("{}Found HomeKit accessory mapping for thing {}: {}", LOG_CMD, thingId, accessory.getUID());
            console.println("HomeKit Accessory:");
            console.println("  UID: " + accessory.getUID());
            console.println("  Type: " + accessory.getClass().getSimpleName());
            console.println("  Services:");
            accessory.getServices().forEach(service -> {
                logger.debug("{}Service: {} (ID: {})", LOG_DEBUG, service.getType(), service.getInstanceId());
                console.println("    " + service.getType());
                console.println("      Characteristics:");
                service.getCharacteristics().forEach(characteristic -> {
                    logger.debug("{}Characteristic: {} (tag: {}, value: {})", LOG_DEBUG, characteristic.getType(),
                            characteristic.getTag(), characteristic.getValue());
                    console.println("        " + characteristic.getType());
                    console.println("          Tag: " + characteristic.getTag());
                    console.println("          Value: " + characteristic.getValue());
                });
            });
        });

        // Get channels
        logger.debug("{}Retrieving channels for thing: {}", LOG_DEBUG, thingId);
        console.println("Channels:");
        thing.getChannels().forEach(channel -> {
            logger.debug("{}Found channel: {}", LOG_DEBUG, channel.getUID());
            console.println("  " + channel.getUID());
            Optional<Map<String, Object>> channelConfig = configManager.getConfiguration(channel.getUID(),
                    ConfigurationType.CHANNEL);
            if (channelConfig.isPresent()) {
                @SuppressWarnings("null") // Optional.get() is safe after isPresent() check
                Map<String, Object> channelConfigMap = channelConfig.get();
                logger.debug("{}Found configuration for channel {}: {} entries", LOG_DEBUG, channel.getUID(),
                        channelConfigMap.size());
                console.println("    Configuration:");
                channelConfigMap.forEach((k, v) -> console.println("      " + k + ": " + v));
            } else {
                logger.debug("{}No configuration found for channel: {}", LOG_DEBUG, channel.getUID());
            }

            // Get linked items
            logger.debug("{}Retrieving linked items for channel: {}", LOG_DEBUG, channel.getUID());
            console.println("    Linked Items:");
            itemChannelLinkRegistry.getLinks(channel.getUID()).forEach(link -> {
                logger.debug("{}Found linked item: {}", LOG_DEBUG, link.getItemName());
                console.println("      " + link.getItemName());
                Optional<Map<String, Object>> linkConfig = configManager.getConfiguration(new ChannelUID(link.getUID()),
                        ConfigurationType.CHANNEL);
                if (linkConfig.isPresent()) {
                    @SuppressWarnings("null") // Optional.get() is safe after isPresent() check
                    Map<String, Object> linkConfigMap = linkConfig.get();
                    logger.debug("{}Found configuration for link {}: {} entries", LOG_DEBUG, link.getUID(),
                            linkConfigMap.size());
                    console.println("        Configuration:");
                    linkConfigMap.forEach((k, v) -> console.println("          " + k + ": " + v));
                } else {
                    logger.debug("{}No configuration found for link: {}", LOG_DEBUG, link.getUID());
                }
            });
        });
        logger.debug("{}Completed showing mapping for thing: {}", LOG_CMD, thingId);
    }

    /**
     * Shows the HomeKit accessories exposed by an accessory server.
     * 
     * This method displays detailed information about a server's HomeKit configuration,
     * including its accessories, services, and characteristics.
     *
     * @param serverId The ID of the server to show
     * @param console The console to write output to
     */
    private void showServer(String serverId, Console console) {
        logger.debug("{}Showing server: {}", LOG_CMD, serverId);
        @SuppressWarnings("null") // accessoryServerRegistry.get() can return null, which is checked below
        HomekitAccessoryServer server = accessoryServerRegistry.get(new HomekitAccessoryServerUIDImpl(serverId));
        if (server == null) {
            logger.warn("{}Server not found: {}", LOG_WARN, serverId);
            console.println("Server not found: " + serverId);
            return;
        }

        logger.debug("{}Found server: {} (type: {}, port: {}, paired: {})", LOG_DEBUG, serverId,
                server.getClass().getSimpleName(), server.getPort(), server.isPaired());
        console.println("Accessory Server: " + serverId);
        console.println("Type: " + server.getClass().getSimpleName());
        console.println("Port: " + server.getPort());
        console.println("Setup Code: " + server.getSetupCode());
        console.println("State: " + (server.isPaired() ? "Paired" : "Unpaired"));

        // Get configuration
        logger.debug("{}Retrieving configuration for server: {}", LOG_DEBUG, serverId);
        Optional<Map<String, Object>> config = configManager
                .getConfiguration(new HomekitUID("homekit:server:" + serverId), ConfigurationType.BRIDGE);
        if (config.isPresent()) {
            @SuppressWarnings("null") // Optional.get() is safe after isPresent() check
            Map<String, Object> configMap = config.get();
            logger.debug("{}Found configuration for server {}: {} entries", LOG_DEBUG, serverId, configMap.size());
            console.println("Configuration:");
            configMap.forEach((k, v) -> console.println("  " + k + ": " + v));
        } else {
            logger.debug("{}No configuration found for server: {}", LOG_DEBUG, serverId);
        }

        // Get accessories
        logger.debug("{}Retrieving accessories for server: {}", LOG_DEBUG, serverId);
        console.println("Accessories:");
        try {
            for (HomekitAccessory accessory : server.getAccessories()) {
                logger.debug("{}Found accessory on server {}: {}", LOG_CMD, serverId, accessory.getUID());
                console.println("  " + accessory.getUID());
                console.println("    Type: " + accessory.getClass().getSimpleName());
                console.println("    Services:");
                accessory.getServices().forEach(service -> {
                    logger.debug("{}Service: {} (ID: {})", LOG_DEBUG, service.getType(), service.getInstanceId());
                    console.println("      " + service.getType());
                    console.println("        Characteristics:");
                    service.getCharacteristics().forEach(characteristic -> {
                        logger.debug("{}Characteristic: {}", LOG_DEBUG, characteristic.getType());
                        console.println("          " + characteristic.getType());
                    });
                });
            }
            logger.debug("{}Successfully retrieved {} accessories for server: {}", LOG_DEBUG,
                    server.getAccessories().size(), serverId);
        } catch (HomekitAccessoryOperationException e) {
            logger.error("{}Error accessing accessories for server {}: {}", LOG_ERROR, serverId, e.getMessage(), e);
            console.println("Error accessing accessories: " + e.getMessage());
        }
        logger.debug("{}Completed showing server: {}", LOG_CMD, serverId);
    }

    /**
     * Prints a list of all HomeKit accessory servers.
     * 
     * This method displays information about each server, including its type,
     * port, setup code, pairing state, and number of accessories.
     *
     * @param console The console to write output to
     */
    private void printServers(Console console) {
        logger.debug("{}Printing all HomeKit accessory servers", LOG_CMD);
        console.println("HomeKit Accessory Servers:");
        accessoryServerRegistry.getAll().forEach(server -> {
            logger.debug("{}Found server: {} (type: {}, port: {})", LOG_DEBUG, server.getUID(),
                    server.getClass().getSimpleName(), server.getPort());
            console.println("  " + server.getUID());
            console.println("    Type: " + server.getClass().getSimpleName());
            console.println("    Port: " + server.getPort());
            console.println("    Setup Code: " + server.getSetupCode());
            console.println("    State: " + (server.isPaired() ? "Paired" : "Unpaired"));
            try {
                int accessoryCount = server.getAccessories().size();
                logger.debug("{}Server {} has {} accessories", LOG_DEBUG, server.getUID(), accessoryCount);
                console.println("    Accessories: " + accessoryCount);
            } catch (HomekitAccessoryOperationException e) {
                logger.warn("{}Error accessing accessories for server {}: {}", LOG_WARN, server.getUID(),
                        e.getMessage());
                console.println("    Error accessing accessories: " + e.getMessage());
            }
        });
        logger.debug("{}Completed printing servers", LOG_CMD);
    }

    /**
     * Prints a list of all HomeKit accessories exposed by bridges.
     * 
     * This method displays information about accessories from the item bridge,
     * thing bridge, and accessory bridge.
     *
     * @param console The console to write output to
     */
    private void printBridgedAccessories(Console console) {
        logger.debug("{}Printing all bridged HomeKit accessories", LOG_CMD);
        console.println("Bridged HomeKit Accessories:");
        console.println("----------------------------");

        // Print accessories from item bridge
        logger.debug("{}Retrieving accessories from item bridge", LOG_DEBUG);
        console.println("\nItem Bridge Accessories:");
        itemBridge.getItems().forEach(item -> {
            logger.debug("{}Checking item for accessory mapping: {}", LOG_DEBUG, item.getName());
            itemBridge.getMappedAccessory(item.getName()).ifPresent(accessory -> {
                logger.debug("{}Found accessory for item {}: {}", LOG_DEBUG, item.getName(), accessory.getUID());
                printAccessoryDetails(console, accessory);
            });
        });

        // Print accessories from thing bridge
        logger.debug("{}Retrieving accessories from thing bridge", LOG_DEBUG);
        console.println("\nThing Bridge Accessories:");
        thingBridge.getThings().forEach(thing -> {
            logger.debug("{}Checking thing for accessory mapping: {}", LOG_DEBUG, thing.getUID());
            thingBridge.getMappedAccessory(thing.getUID()).ifPresent(accessory -> {
                logger.debug("{}Found accessory for thing {}: {}", LOG_DEBUG, thing.getUID(), accessory.getUID());
                printAccessoryDetails(console, accessory);
            });
        });

        // Print accessories from the accessory bridge
        logger.debug("{}Retrieving accessories from accessory bridge", LOG_DEBUG);
        console.println("\nAccessory Bridge Accessories:");
        accessoryBridge.getAccessories().forEach(accessory -> {
            logger.debug("{}Found accessory in accessory bridge: {}", LOG_DEBUG, accessory.getUID());
            printAccessoryDetails(console, accessory);
        });
        logger.debug("{}Completed printing bridged accessories", LOG_CMD);
    }

    /**
     * Prints a list of all HomeKit accessories.
     * 
     * This method displays detailed information about each accessory,
     * including its label, ID, manufacturer, model, and services.
     *
     * @param console The console to write output to
     */
    private void printAccessories(Console console) {
        logger.debug("{}Printing all HomeKit accessories", LOG_CMD);
        console.println("HomeKit Accessories:");
        console.println("--------------------");
        accessoryRegistry.getAll().forEach(accessory -> {
            logger.debug("{}Found accessory: {} (label: {})", LOG_DEBUG, accessory.getUID(), accessory.getLabel());
            printAccessoryDetails(console, accessory);
        });
        logger.debug("{}Completed printing accessories", LOG_CMD);
    }

    /**
     * Prints a list of all HomeKit pairings.
     * 
     * This method displays information about pairings for each server,
     * including pairing IDs and public keys.
     *
     * @param console The console to write output to
     */
    private void printPairings(Console console) {
        logger.debug("{}Printing all HomeKit pairings", LOG_CMD);
        accessoryServerRegistry.getAll().forEach(server -> {
            console.println("  Server: " + server.getUID());
            try {
                server.getPairings().forEach(pairing -> {
                    @SuppressWarnings("null") // Base64.getEncoder().encodeToString() always returns non-null
                    String destinationIdEncoded = Base64.getEncoder().encodeToString(pairing.getDestinationId());
                    @SuppressWarnings("null") // Base64.getEncoder().encodeToString() always returns non-null
                    String publicKeyEncoded = Base64.getEncoder().encodeToString(pairing.getPublicKey());
                    logger.debug("{}Found pairing for server {}: {}", LOG_CMD, server.getUID(), destinationIdEncoded);
                    console.println("    Pairing ID: " + destinationIdEncoded);
                    console.println("      Public Key: " + publicKeyEncoded);
                });
            } catch (HomekitServerException e) {
                logger.error("{}Error getting pairings from server {}: {}", LOG_ERROR, server.getUID(), e.getMessage(),
                        e);
                console.println("Error getting pairings from server: " + e.getMessage());
            }
        });
    }

    /**
     * Adds a new pairing to a HomeKit server.
     * 
     * This method adds a new pairing to the specified server using the provided setup code.
     * The setup code should be in the format "XXX-XX-XXX" where X is a digit.
     *
     * @param serverId The ID of the server to add the pairing to
     * @param setupCode The setup code for the new pairing
     * @param console The console to write output to
     */
    private void addPairing(String serverId, String setupCode, Console console) {
        logger.debug("{}Adding pairing to server {} with setup code {}", LOG_CMD, serverId, setupCode);
        @SuppressWarnings("null") // accessoryServerRegistry.get() can return null, which is checked below
        HomekitAccessoryServer server = accessoryServerRegistry.get(new HomekitAccessoryServerUIDImpl(serverId));
        if (server == null) {
            logger.warn("{}Server not found: {}", LOG_WARN, serverId);
            console.println("Server not found: " + serverId);
            return;
        }

        server.setSetupCode(setupCode);
        try {
            server.pairSetup();
            logger.info("{}Successfully added pairing to server: {}", LOG_STATE, serverId);
            console.println("Successfully added pairing to server: " + serverId);
        } catch (HomekitServerException e) {
            logger.error("{}Error adding pairing to server {}: {}", LOG_ERROR, serverId, e.getMessage(), e);
            console.println("Error adding pairing to server: " + e.getMessage());
        }
    }

    /**
     * Removes a pairing from a HomeKit server.
     * 
     * This method removes an existing pairing from the specified server using the pairing ID.
     * The pairing ID should be provided in base64 format.
     *
     * @param serverId The ID of the server to remove the pairing from
     * @param pairingId The ID of the pairing to remove
     * @param console The console to write output to
     */
    private void removePairing(String serverId, String pairingId, Console console) {
        logger.debug("{}Removing pairing {} from server {}", LOG_CMD, pairingId, serverId);
        @SuppressWarnings("null") // accessoryServerRegistry.get() can return null, which is checked below
        HomekitAccessoryServer server = accessoryServerRegistry.get(new HomekitAccessoryServerUIDImpl(serverId));
        if (server == null) {
            logger.warn("{}Server not found: {}", LOG_WARN, serverId);
            console.println("Server not found: " + serverId);
            return;
        }

        try {
            @SuppressWarnings("null") // Base64.getDecoder().decode() always returns non-null byte array
            byte[] decodedPairingId = Base64.getDecoder().decode(pairingId);
            server.removePairing(decodedPairingId);
            logger.info("{}Successfully removed pairing from server: {}", LOG_STATE, serverId);
            console.println("Successfully removed pairing from server: " + serverId);
        } catch (HomekitServerException e) {
            logger.error("{}Error removing pairing from server {}: {}", LOG_ERROR, serverId, e.getMessage(), e);
            console.println("Error removing pairing from server: " + e.getMessage());
        }
    }

    /**
     * Prints detailed information about a HomeKit accessory.
     * 
     * This method displays comprehensive information about an accessory,
     * including its label, ID, manufacturer, model, serial number,
     * and all services with their characteristics.
     *
     * @param console The console to write output to
     * @param accessory The accessory to print details for
     */
    private void printAccessoryDetails(Console console, HomekitAccessory accessory) {
        logger.debug("{}Printing details for accessory: {}", LOG_DEBUG, accessory.getUID());
        console.println("Accessory Details:");
        console.println("-----------------");
        console.println("Label: " + accessory.getLabel());
        console.println("ID: " + accessory.getAccessoryId());
        console.println("Manufacturer: " + accessory.getManufacturer());
        console.println("Model: " + accessory.getModel());
        console.println("Serial Number: " + accessory.getSerialNumber());
        logger.debug("{}Accessory {} has {} services", LOG_DEBUG, accessory.getUID(), accessory.getServices().size());
        console.println("\nServices:");
        for (HomekitService service : accessory.getServices()) {
            logger.debug("{}Service: {} (ID: {}, {} characteristics)", LOG_DEBUG, service.getType(),
                    service.getInstanceId(), service.getCharacteristics().size());
            console.println("  " + service.getType() + " (ID: " + service.getInstanceId() + ")");
            console.println("    Characteristics:");
            for (HomekitCharacteristic<?> characteristic : service.getCharacteristics()) {
                logger.debug("{}Characteristic: {}", LOG_DEBUG, characteristic.getType());
                console.println("      " + characteristic.getType());
            }
        }
        logger.debug("{}Completed printing details for accessory: {}", LOG_DEBUG, accessory.getUID());
    }
}
