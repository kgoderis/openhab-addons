package org.openhab.io.homekit.hap.impl.characteristics.light;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.LightSensor;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.FloatCharacteristic;
import java.util.concurrent.CompletableFuture;

public class AmbientLightLevelCharacteristic extends FloatCharacteristic
    implements EventableCharacteristic {

  private final LightSensor lightSensor;

  public AmbientLightLevelCharacteristic(LightSensor lightSensor) {
    super(
        "0000006B-0000-1000-8000-0026BB765291",
        false,
        true,
        "Current ambient light level",
        0.0001,
        100000,
        0.0001,
        "lux");
    this.lightSensor = lightSensor;
  }

  @Override
  protected void setValue(Double value) throws Exception {
    // Read Only
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    lightSensor.subscribeCurrentAmbientLightLevel(callback);
  }

  @Override
  public void unsubscribe() {
    lightSensor.unsubscribeCurrentAmbientLightLevel();
  }

  @Override
  protected CompletableFuture<Double> getDoubleValue() {
    return lightSensor.getCurrentAmbientLightLevel();
  }
}
