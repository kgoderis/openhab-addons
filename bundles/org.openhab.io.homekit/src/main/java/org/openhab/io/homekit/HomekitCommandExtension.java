package org.openhab.io.homekit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.AccessoryServerRegistry;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.api.ManagedCharacteristic;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.library.accessory.ThingAccessory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Console commands for interacting with the HomeKit integration
 *
 * @author Andy Lintner - Initial contribution
 */
@Component(service = ConsoleCommandExtension.class)
public class HomekitCommandExtension extends AbstractConsoleCommandExtension {
    private static final String CMD_HOMEKIT = "homekit";
    private static final String CMD_SERVER = "server";
    private static final String CMD_ACCESSORY = "accessory";
    private static final String CMD_PAIRING = "pairing";

    private static final String SUBCMD_ADD = "add";
    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_SHOW = "show";
    private static final String SUBCMD_CLEAR = "clear";
    private static final String SUBCMD_REMOVE = "remove";
    private static final String SUBCMD_TRIGGER = "trigger";
    private static final String SUBCMD_DISABLE = "disable";
    private static final String SUBCMD_ENABLE = "enable";

    private final Logger logger = LoggerFactory.getLogger(HomekitCommandExtension.class);

    private final ThingRegistry thingRegistry;
    private final AccessoryServerRegistry accessoryServerRegistry;
    private final AccessoryRegistry accessoryRegistry;
    private final NotificationRegistry notificationRegistry;

    @Activate
    public HomekitCommandExtension(@Reference ThingRegistry thingRegistry,
            @Reference AccessoryServerRegistry accessoryServerRegistry, @Reference AccessoryRegistry accessoryRegistry,
            @Reference NotificationRegistry notificationRegistry) {
        super(CMD_HOMEKIT, "Interact with HomeKit.");
        this.thingRegistry = thingRegistry;
        this.accessoryServerRegistry = accessoryServerRegistry;
        this.notificationRegistry = notificationRegistry;
        this.accessoryRegistry = accessoryRegistry;
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case CMD_SERVER: {
                    executeServer(args, console);
                    break;
                }
                case CMD_ACCESSORY: {
                    executeAccessory(args, console);
                    break;
                }
                case CMD_PAIRING: {
                    executePairing(args, console);
                    break;
                }
                default: {
                    console.println("Unknown command '" + subCommand + "'");
                    printUsage(console);
                    break;
                }
            }
        } else {
            printUsage(console);
        }
    }

    private void executeServer(String[] args, Console console) {
        if (args.length > 1) {
            String actionCommand = args[1];
            switch (actionCommand) {

                default:
                    console.println("Unknown command '" + actionCommand + "'");
                    printUsage(console);
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    private void executeAccessory(String[] args, Console console) {
        if (args.length > 1) {
            String actionCommand = args[1];
            switch (actionCommand) {
                case SUBCMD_LIST: {
                    printAccessories(console);
                    break;
                }
                default:
                    console.println("Unknown command '" + actionCommand + "'");
                    printUsage(console);
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    private void executePairing(String[] args, Console console) {
        if (args.length > 1) {
            String actionCommand = args[1];
            switch (actionCommand) {
                case SUBCMD_ADD:
                    if (args.length > 3) {
                        try {
                            String thingUID = args[2];
                            String setupCode = args[3];
                            addPairing(thingUID, setupCode, console);
                        } catch (IllegalArgumentException e) {
                            console.println("'" + args[1] + "' is no valid thing UID.");
                        }
                    } else {
                        console.println("<thingUID> <setupCode> are required as arguments");
                        printUsage(console);
                    }
                    break;
                case SUBCMD_CLEAR:
                    clearHomekitPairings(console);
                    break;
                default:
                    console.println("Unknown command '" + actionCommand + "'");
                    printUsage(console);
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] {
                buildCommandUsage(CMD_ACCESSORY + " " + SUBCMD_LIST, "list all the accessories"),
                buildCommandUsage(CMD_PAIRING + " " + SUBCMD_CLEAR, "removes all pairings with Homekit clients"), });
    }

    private void printAccessories(Console console) {
        Collection<ManagedAccessory> accessories = accessoryRegistry.getAll();

        if (accessories.isEmpty()) {
            console.println("No accessories found.");
        }

        for (Iterator<ManagedAccessory> iter = accessories.iterator(); iter.hasNext();) {
            ManagedAccessory accessory = iter.next();

            if (accessory instanceof ThingAccessory) {
                console.println(String.format("Accessory %s (Type=%s, Label=%s, Thing=%s)",
                        accessory.getUID().toString(), accessory.getClass().getSimpleName(), accessory.getLabel(),
                        ((ThingAccessory) accessory).getThingUID()));
            } else {
                console.println(String.format("Accessory %s (Type=%s, Label=%s)", accessory.getUID().toString(),
                        accessory.getClass().getSimpleName(), accessory.getLabel()));
            }
            for (Service service : accessory.getServices()) {
                console.println(String.format("     Service %s (Type=%s, HAP=%s, Name=%s)",
                        ((ManagedService) service).getUID().toString(), service.getClass().getSimpleName(),
                        service.getInstanceType(), ((ManagedService) service).getName()));
                for (Characteristic characteristic : service.getCharacteristics()) {
                    if (((ManagedCharacteristic<?>) characteristic).getChannelUID() != null) {
                        console.println(String.format("         Characteristic %s (Type=%s, HAP=%s, Channel=%s)",
                                ((ManagedCharacteristic<?>) characteristic).getUID().toString(),
                                characteristic.getClass().getSimpleName(), characteristic.getInstanceType(),
                                ((ManagedCharacteristic<?>) characteristic).getChannelUID()));
                    } else {
                        console.println(String.format("         Characteristic %s (Type=%s, HAP=%s)",
                                ((ManagedCharacteristic<?>) characteristic).getUID().toString(),
                                characteristic.getClass().getSimpleName(), characteristic.getInstanceType()));
                    }
                }
            }

            if (iter.hasNext()) {
                console.println("");
                console.println("--- --- --- --- ---");
                console.println("");
            }
        }
    }

    private void clearHomekitPairings(Console console) {
        try {
            console.println("Cleared homekit pairings");
        } catch (Exception e) {
            logger.warn("Could not clear homekit pairings", e);
        }
    }

    private void addPairing(String thingUID, String setupCode, Console console) {

    }

    // private MaxCubeBridgeHandler getHandler(String thingId) {
    // MaxCubeBridgeHandler handler = null;
    // try {
    // ThingUID bridgeUID = new ThingUID(thingId);
    // Thing thing = thingRegistry.get(bridgeUID);
    // if ((thing != null) && (thing.getHandler() != null)
    // && (thing.getHandler() instanceof MaxCubeBridgeHandler)) {
    // handler = (MaxCubeBridgeHandler) thing.getHandler();
    // }
    // } catch (Exception e) {
    // handler = null;
    // }
    // return handler;
    // }
    //
    // private List<Thing> findDevices(Set<ThingTypeUID> deviceTypes) {
    // List<Thing> devs = new ArrayList<Thing>();
    // for (Thing thing : thingRegistry.getAll()) {
    // if (deviceTypes.contains(thing.getThingTypeUID())) {
    // devs.add(thing);
    // }
    // }
    // return devs;
    // }
}
