package org.openhab.io.homekit.hap.impl.json;

import  org.openhab.io.homekit.hap.characteristics.Characteristic;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import  org.openhab.io.homekit.hap.impl.HomekitRegistry;
import  org.openhab.io.homekit.hap.impl.connections.SubscriptionManager;
import  org.openhab.io.homekit.hap.impl.http.HomekitClientConnection;
import  org.openhab.io.homekit.hap.impl.http.HttpRequest;
import  org.openhab.io.homekit.hap.impl.http.HttpResponse;
import  org.openhab.io.homekit.hap.impl.responses.NotFoundResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import javax.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharacteristicsController {

  Logger logger = LoggerFactory.getLogger(CharacteristicsController.class);

  private final HomekitRegistry registry;
  private final SubscriptionManager subscriptions;

  public CharacteristicsController(HomekitRegistry registry, SubscriptionManager subscriptions) {
    this.registry = registry;
    this.subscriptions = subscriptions;
  }

  public HttpResponse get(HttpRequest request) throws Exception {
    String uri = request.getUri();
    // Characteristics are requested with /characteristics?id=1.1,2.1,3.1
    String query = uri.substring("/characteristics?id=".length());
    String[] ids = query.split(",");
    JsonArrayBuilder characteristics = Json.createArrayBuilder();
    for (String id : ids) {
      String[] parts = id.split("\\.");
      if (parts.length != 2) {
        logger.error("Unexpected characteristics request: " + uri);
        return new NotFoundResponse();
      }
      int aid = Integer.parseInt(parts[0]);
      int iid = Integer.parseInt(parts[1]);
      JsonObjectBuilder characteristic = Json.createObjectBuilder();
      Map<Integer, Characteristic> characteristicMap = registry.getCharacteristics(aid);
      if (!characteristicMap.isEmpty()) {
        Characteristic targetCharacteristic = characteristicMap.get(iid);
        if (targetCharacteristic != null) {
          targetCharacteristic.supplyValue(characteristic);

          characteristics.add(characteristic.add("aid", aid).add("iid", iid).build());
        } else {
          logger.warn(
              "Accessory " + aid + " does not have characteristic " + iid + "Request: " + uri);
        }
      } else {
        logger.warn(
            "Accessory " + aid + " has no characteristics or does not exist. Request: " + uri);
      }
    }
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Json.createWriter(baos)
          .write(
              Json.createObjectBuilder().add("characteristics", characteristics.build()).build());
      return new HapJsonResponse(baos.toByteArray());
    }
  }

  public HttpResponse put(HttpRequest request, HomekitClientConnection connection)
      throws Exception {
    subscriptions.batchUpdate();
    try {
      try (ByteArrayInputStream bais = new ByteArrayInputStream(request.getBody())) {
        JsonArray jsonCharacteristics =
            Json.createReader(bais).readObject().getJsonArray("characteristics");
        for (JsonValue value : jsonCharacteristics) {
          JsonObject jsonCharacteristic = (JsonObject) value;
          int aid = jsonCharacteristic.getInt("aid");
          int iid = jsonCharacteristic.getInt("iid");
          Characteristic characteristic = registry.getCharacteristics(aid).get(iid);

          if (jsonCharacteristic.containsKey("value")) {
            characteristic.setValue(jsonCharacteristic.get("value"));
          }
          if (jsonCharacteristic.containsKey("ev")
              && characteristic instanceof EventableCharacteristic) {
            if (jsonCharacteristic.getBoolean("ev")) {
              subscriptions.addSubscription(
                  aid, iid, (EventableCharacteristic) characteristic, connection);
            } else {
              subscriptions.removeSubscription(
                  (EventableCharacteristic) characteristic, connection);
            }
          }
        }
      }
    } finally {
      subscriptions.completeUpdateBatch();
    }
    return new HapJsonNoContentResponse();
  }
}
