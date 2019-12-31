package org.openhab.io.homekit.hap.impl.characteristics.windowcovering;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.BasicWindowCovering;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.IntegerCharacteristic;
import java.util.concurrent.CompletableFuture;

public class TargetPositionCharacteristic extends IntegerCharacteristic
    implements EventableCharacteristic {

  private final BasicWindowCovering windowCovering;

  public TargetPositionCharacteristic(BasicWindowCovering windowCovering) {
    super("0000007C-0000-1000-8000-0026BB765291", true, true, "The target position", 0, 100, "%");
    this.windowCovering = windowCovering;
  }

  @Override
  protected void setValue(Integer value) throws Exception {
    windowCovering.setTargetPosition(value);
  }

  @Override
  protected CompletableFuture<Integer> getValue() {
    return windowCovering.getTargetPosition();
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    windowCovering.subscribeTargetPosition(callback);
  }

  @Override
  public void unsubscribe() {
    windowCovering.unsubscribeTargetPosition();
  }
}
