package org.openhab.binding.atsadvanced.poc.app;

import java.util.List;
import java.util.Locale;

import org.openhab.binding.atsadvanced.poc.client.GatewayClient;
import org.openhab.binding.atsadvanced.poc.control.AreaControl;
import org.openhab.binding.atsadvanced.poc.control.AreaControl.AreaStatus;
import org.openhab.binding.atsadvanced.poc.prose.AtsProse;
import org.openhab.binding.atsadvanced.poc.session.PanelSession;
import org.openhab.binding.atsadvanced.poc.session.PanelSession.NamedObject;

/**
 * CLI POC — runs outside openHAB.
 *
 * Usage:
 *   mvn -q exec:java -Dexec.args="--gateway http://127.0.0.1:18137 --panel 192.168.0.11:32000 --pin PIN status"
 *   ... areas | status [n] | arm n | disarm n
 */
public final class AtsPocMain {

    public static void main(String[] args) throws Exception {
        String gateway = "http://127.0.0.1:18137";
        String panel = "192.168.0.11:32000";
        String pin = null;
        String command = "status";
        Integer areaArg = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--gateway" -> gateway = args[++i];
                case "--panel" -> panel = args[++i];
                case "--pin" -> pin = args[++i];
                case "areas", "status", "arm", "disarm", "health" -> {
                    command = args[i];
                    if (i + 1 < args.length && args[i + 1].matches("\\d+")) {
                        areaArg = Integer.parseInt(args[++i]);
                    }
                }
                default -> {
                    if (!args[i].startsWith("--")) {
                        command = args[i];
                    }
                }
            }
        }

        if ("health".equals(command)) {
            try (GatewayClient client = new GatewayClient(gateway)) {
                System.out.println(client.health().status + " configured=" + client.health().configured + " "
                        + client.health().runtime);
            }
            return;
        }

        if (pin == null || pin.isBlank()) {
            System.err.println("Missing --pin (use a PIN that is not already logged in elsewhere)");
            System.exit(2);
        }

        String[] hp = panel.split(":");
        String host = hp[0];
        int port = hp.length > 1 ? Integer.parseInt(hp[1]) : 32000;

        AtsProse prose = new AtsProse();
        try (GatewayClient client = new GatewayClient(gateway);
                PanelSession session = new PanelSession(client, prose)) {

            session.addEventListener(msg -> {
                if ("msg.MONITOR".equals(msg.name)) {
                    Object text = msg.property("event_text");
                    System.out.println("[monitor] " + (text != null ? text : msg.name));
                }
            });

            System.out.println(prose.action("login") + "…");
            session.connect(host, port, pin);
            System.out.println(prose.state("online") + " (" + session.getState() + ")");

            AreaControl areas = new AreaControl(session, prose);

            switch (command.toLowerCase(Locale.ROOT)) {
                case "areas" -> {
                    List<NamedObject> list = session.listAreas();
                    for (NamedObject a : list) {
                        try {
                            AreaStatus st = areas.getStatus(a.number());
                            System.out.printf(Locale.ROOT, "%d  %s  —  %s%n", a.number(), a.name(), st.summary());
                        } catch (Exception ex) {
                            System.out.printf(Locale.ROOT, "%d  %s  —  (status unavailable: %s)%n", a.number(),
                                    a.name(), ex.getMessage());
                        }
                    }
                }
                case "arm" -> {
                    int n = areaArg != null ? areaArg : 1;
                    System.out.println(prose.action("arm") + " " + n);
                    areas.setArea(n);
                    System.out.println(areas.getStatus(n));
                }
                case "disarm" -> {
                    int n = areaArg != null ? areaArg : 1;
                    System.out.println(prose.action("disarm") + " " + n);
                    areas.unsetArea(n);
                    System.out.println(areas.getStatus(n));
                }
                default -> {
                    if (areaArg != null) {
                        System.out.println(areas.getStatus(areaArg));
                    } else {
                        for (NamedObject a : session.listAreas()) {
                            try {
                                System.out.printf(Locale.ROOT, "%d  %s  —  %s%n", a.number(), a.name(),
                                        areas.getStatus(a.number()).summary());
                            } catch (Exception ex) {
                                System.out.printf(Locale.ROOT, "%d  %s  —  (status unavailable: %s)%n", a.number(),
                                        a.name(), ex.getMessage());
                            }
                        }
                    }
                }
            }

            System.out.println(prose.action("logout") + "…");
        }
    }
}
