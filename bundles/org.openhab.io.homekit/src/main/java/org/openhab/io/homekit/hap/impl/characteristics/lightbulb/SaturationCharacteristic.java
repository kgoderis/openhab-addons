package org.openhab.io.homekit.hap.impl.characteristics.lightbulb;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.ColorfulLightbulb;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.FloatCharacteristic;
import java.util.concurrent.CompletableFuture;

public class SaturationCharacteristic extends FloatCharacteristic
    implements EventableCharacteristic {

  private final ColorfulLightbulb lightbulb;

  public SaturationCharacteristic(ColorfulLightbulb lightbulb) {
    super(
        "0000002F-0000-1000-8000-0026BB765291",
        true,
        true,
        "Adjust saturation of the light",
        0,
        100,
        1,
        "%");
    this.lightbulb = lightbulb;
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    lightbulb.subscribeSaturation(callback);
  }

  @Override
  public void unsubscribe() {
    lightbulb.unsubscribeSaturation();
  }

  @Override
  protected void setValue(Double value) throws Exception {
    lightbulb.setSaturation(value);
  }

  @Override
  protected CompletableFuture<Double> getDoubleValue() {
    return lightbulb.getSaturation();
  }
}
