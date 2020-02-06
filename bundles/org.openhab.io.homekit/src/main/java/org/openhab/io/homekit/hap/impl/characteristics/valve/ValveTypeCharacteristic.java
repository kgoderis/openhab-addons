package org.openhab.io.homekit.hap.impl.characteristics.valve;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.Valve;
import  org.openhab.io.homekit.hap.characteristics.EnumCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import java.util.concurrent.CompletableFuture;

public class ValveTypeCharacteristic extends EnumCharacteristic implements EventableCharacteristic {

  private final Valve valve;

  public ValveTypeCharacteristic(Valve valve) {
    super("000000D5-0000-1000-8000-0026BB765291", false, true, "Valve Type", 3);
    this.valve = valve;
  }

  @Override
  protected void setValue(Integer value) throws Exception {
    // Read only
  }

  @Override
  protected CompletableFuture<Integer> getValue() {
    return valve.getValveType().thenApply(v -> v.getCode());
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    valve.subscribeValveType(callback);
  }

  @Override
  public void unsubscribe() {
    valve.unsubscribeValveType();
  }
}
