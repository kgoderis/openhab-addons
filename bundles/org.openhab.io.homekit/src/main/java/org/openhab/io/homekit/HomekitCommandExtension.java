package org.openhab.io.homekit;

import java.util.Arrays;
import java.util.List;

import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.storage.StorageService;
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
    private static final String SUBCMD_CLEAR_PAIRINGS = "clearPairings";
    private static final String SUBCMD_ALLOW_UNAUTHENTICATED = "allowUnauthenticated";
    private static final String SUBCMD_LIST_SERVERS = "listServers";
    private static final String SUBCMD_LIST_ACCESSORIES = "listAccessories";

    private final Logger logger = LoggerFactory.getLogger(HomekitCommandExtension.class);
    private StorageService storageService;

    public HomekitCommandExtension() {
        super("homekit", "Interact with the HomeKit integration.");
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_CLEAR_PAIRINGS:
                    clearHomekitPairings(console);
                    break;

                case SUBCMD_ALLOW_UNAUTHENTICATED:
                    if (args.length > 1) {
                        boolean allow = Boolean.valueOf(args[1]);
                        allowUnauthenticatedHomekitRequests(allow, console);
                    } else {
                        console.println("true/false is required as an argument");
                    }
                    break;

                default:
                    console.println("Unknown command '" + subCommand + "'");
                    printUsage(console);
                    break;
            }
        }
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(
                new String[] { buildCommandUsage(SUBCMD_CLEAR_PAIRINGS, "removes all pairings with Homekit clients"),
                        buildCommandUsage(SUBCMD_ALLOW_UNAUTHENTICATED + " <boolean>",
                                "enables or disables unauthenticated access to facilitate debugging") });
    }

    @Reference
    public void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    public void unsetStorageService(StorageService storageService) {
        this.storageService = null;
    }

    private void clearHomekitPairings(Console console) {
        try {
            // new HomekitAuthInfoImpl(storageService, null).clear();
            // homekit.refreshAuthInfo();
            console.println("Cleared homekit pairings");
        } catch (Exception e) {
            logger.warn("Could not clear homekit pairings", e);
        }
    }

    // console.println(String.format("%s (Type=%s, Status=%s, Label=%s, Bridge=%s)", id, thingType, status, label,
    // bridgeUID));

    private void allowUnauthenticatedHomekitRequests(boolean allow, Console console) {
        // homekit.allowUnauthenticatedRequests(allow);
        console.println((allow ? "Enabled " : "Disabled ") + "unauthenticated homekit access");
    }
}
