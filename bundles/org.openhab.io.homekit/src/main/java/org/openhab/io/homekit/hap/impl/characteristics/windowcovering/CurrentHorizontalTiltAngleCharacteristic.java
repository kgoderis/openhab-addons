package org.openhab.io.homekit.hap.impl.characteristics.windowcovering;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.HorizontalTiltingWindowCovering;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.IntegerCharacteristic;
import java.util.concurrent.CompletableFuture;

public class CurrentHorizontalTiltAngleCharacteristic extends IntegerCharacteristic
    implements EventableCharacteristic {

  private final HorizontalTiltingWindowCovering windowCovering;

  public CurrentHorizontalTiltAngleCharacteristic(HorizontalTiltingWindowCovering windowCovering) {
    super(
        "0000006C-0000-1000-8000-0026BB765291",
        false,
        true,
        "The current horizontal tilt angle",
        -90,
        90,
        "Arc Degree");
    this.windowCovering = windowCovering;
  }

  @Override
  protected void setValue(Integer value) throws Exception {
    // Read Only
  }

  @Override
  protected CompletableFuture<Integer> getValue() {
    return windowCovering.getCurrentHorizontalTiltAngle();
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    windowCovering.subscribeCurrentHorizontalTiltAngle(callback);
  }

  @Override
  public void unsubscribe() {
    windowCovering.unsubscribeCurrentHorizontalTiltAngle();
  }
}
