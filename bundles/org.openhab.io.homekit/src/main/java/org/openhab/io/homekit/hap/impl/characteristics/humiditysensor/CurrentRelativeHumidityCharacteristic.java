package org.openhab.io.homekit.hap.impl.characteristics.humiditysensor;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.HumiditySensor;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.FloatCharacteristic;
import java.util.concurrent.CompletableFuture;

public class CurrentRelativeHumidityCharacteristic extends FloatCharacteristic
    implements EventableCharacteristic {

  private final HumiditySensor sensor;

  public CurrentRelativeHumidityCharacteristic(HumiditySensor sensor) {
    super(
        "00000010-0000-1000-8000-0026BB765291",
        false,
        true,
        "Current relative humidity",
        0,
        100,
        0.1,
        "%");
    this.sensor = sensor;
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    sensor.subscribeCurrentRelativeHumidity(callback);
  }

  @Override
  public void unsubscribe() {
    sensor.unsubscribeCurrentRelativeHumidity();
  }

  @Override
  protected void setValue(Double value) throws Exception {
    // Read Only
  }

  @Override
  protected CompletableFuture<Double> getDoubleValue() {
    return sensor.getCurrentRelativeHumidity();
  }
}
