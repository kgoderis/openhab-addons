package org.openhab.io.homekit.hap.impl.characteristics.windowcovering;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.BasicWindowCovering;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.IntegerCharacteristic;
import java.util.concurrent.CompletableFuture;

public class CurrentPositionCharacteristic extends IntegerCharacteristic
    implements EventableCharacteristic {

  private final BasicWindowCovering windowCovering;

  public CurrentPositionCharacteristic(BasicWindowCovering windowCovering) {
    super("0000006D-0000-1000-8000-0026BB765291", false, true, "The current position", 0, 100, "%");
    this.windowCovering = windowCovering;
  }

  @Override
  protected void setValue(Integer value) throws Exception {
    // Read Only
  }

  @Override
  protected CompletableFuture<Integer> getValue() {
    return windowCovering.getCurrentPosition();
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    windowCovering.subscribeCurrentPosition(callback);
  }

  @Override
  public void unsubscribe() {
    windowCovering.unsubscribeCurrentPosition();
  }
}
