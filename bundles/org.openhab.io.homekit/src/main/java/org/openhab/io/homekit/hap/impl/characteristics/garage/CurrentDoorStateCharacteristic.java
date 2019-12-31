package org.openhab.io.homekit.hap.impl.characteristics.garage;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.GarageDoor;
import  org.openhab.io.homekit.hap.characteristics.EnumCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import java.util.concurrent.CompletableFuture;

public class CurrentDoorStateCharacteristic extends EnumCharacteristic
    implements EventableCharacteristic {

  private final GarageDoor door;

  public CurrentDoorStateCharacteristic(GarageDoor door) {
    super("0000000E-0000-1000-8000-0026BB765291", false, true, "Current Door State", 4);
    this.door = door;
  }

  @Override
  protected void setValue(Integer value) throws Exception {
    // Read Only
  }

  @Override
  protected CompletableFuture<Integer> getValue() {
    return door.getCurrentDoorState().thenApply(s -> s.getCode());
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    door.subscribeCurrentDoorState(callback);
  }

  @Override
  public void unsubscribe() {
    door.unsubscribeCurrentDoorState();
  }
}
