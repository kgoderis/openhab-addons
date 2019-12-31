package org.openhab.io.homekit.internal.notification;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.io.HexDump;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.util.FutureCallback;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class NotificationImpl implements Notification {

    protected static final Logger logger = LoggerFactory.getLogger(Notification.class);

    // DeliveringnotificationstocontrollersrequiresthecontrollertoestablishanHAPsessionwiththeaccessory.
    // • Thenotificationregistrationstateofacharacteristicmustnotpersistacrosssessions.
    // • WhenanewHAPsessionisestablishedthenotificationregistrationstateofthatcontrollermustbeʼfalseʼfor all
    // characteristics provided by the accessory.
    // • Awrite-onlycharacteristic(i.e.thecharacteristicpermissionsonlyincludePairedWrite”pw”)mustnotsupport event
    // notifications.
    // • Theaccessorymustsupportregisteringfornotificationsagainstmultiplecharacteristicsinasinglerequest.
    // • Theaccessoryshouldcoalescenotificationswheneverpossible.
    // • Theaccessorymustonlydelivernotificationstothecontrollerforcharacteristicsthatthecontrollerhasregis- tered to
    // receive notifications against.
    // • At any point the controller may deregister for event notifications against a characteristic by setting the ”ev”
    // key to ”false.” The accessory must stop delivering notifications for the deregistered characteristic immediately
    // after receiving the deregister request from the controller.

    @SuppressWarnings("rawtypes")
    Characteristic characteristic;
    HttpConnection connection;
    List<JsonObject> notifications = Collections.synchronizedList(new LinkedList<JsonObject>());

    boolean batchMode = false;

    public NotificationImpl(@SuppressWarnings("rawtypes") Characteristic characteristic, HttpConnection connection) {
        this.characteristic = characteristic;
        this.connection = connection;
    }

    @Override
    public NotificationUID getUID() {
        return new NotificationUID(characteristic.getService().getAccessory().getServer().getId(),
                characteristic.getService().getAccessory().getId(), characteristic.getService().getId(),
                characteristic.getId());
    }

    public void disableBatchMode() {
        this.batchMode = false;
    }

    public void enableBatchMode() {
        this.batchMode = true;
    }

    @Override
    public synchronized void publish(JsonObject notification) {
        if (characteristic.getId() == this.characteristic.getId()) {
            if (batchMode) {
                notifications.add(notification);
            } else {
                JsonArrayBuilder notifications = Json.createArrayBuilder().add(notification);
                publish(notifications);
            }
        }
    }

    @Override
    public synchronized void publish() {
        if (notifications.size() > 0) {
            JsonArrayBuilder notificationsBuilder = Json.createArrayBuilder();

            for (JsonObject notification : notifications) {
                notificationsBuilder.add(notification);
            }

            publish(notificationsBuilder);

            notifications.clear();
        }
    }

    protected synchronized void publish(JsonArrayBuilder arrayBuilder) {
        JsonObjectBuilder builder = Json.createObjectBuilder().add("characteristics", arrayBuilder);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Json.createWriter(baos).write(builder.build());
            byte[] dataBytes = baos.toByteArray();

            HttpFields fields = new HttpFields();
            fields.add("X-HAP-Event", "True");
            fields.add("Content-Type", "application/hap+json");

            MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, HttpStatus.OK_200, "", fields,
                    dataBytes.length);
            FutureCallback callback = new FutureCallback();
            logger.debug("Publishing Notification to connection {}", connection.toString());
            if (dataBytes.length > 0) {
                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                    HexDump.dump(dataBytes, 0, stream, 0);
                    stream.flush();
                    logger.debug("\n{}", stream.toString(StandardCharsets.UTF_8.name()));
                }
            }
            connection.send(info, false, ByteBuffer.wrap(dataBytes), true, callback);
            connection.getGenerator().reset();
            callback.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public Characteristic<?> getCharacteristic() {
        return characteristic;
    }
}
