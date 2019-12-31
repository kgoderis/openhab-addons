package org.openhab.io.homekit.hap.impl.characteristics.lock.mechanism;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.LockMechanism;
import  org.openhab.io.homekit.hap.characteristics.EnumCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import java.util.concurrent.CompletableFuture;

public class CurrentLockMechanismStateCharacteristic extends EnumCharacteristic
    implements EventableCharacteristic {

  private final LockMechanism lock;

  public CurrentLockMechanismStateCharacteristic(LockMechanism lock) {
    super("0000001D-0000-1000-8000-0026BB765291", false, true, "Current lock state", 3);
    this.lock = lock;
  }

  @Override
  protected void setValue(Integer value) throws Exception {
    // Not writable
  }

  @Override
  protected CompletableFuture<Integer> getValue() {
    return lock.getCurrentMechanismState().thenApply(s -> s.getCode());
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    lock.subscribeCurrentMechanismState(callback);
  }

  @Override
  public void unsubscribe() {
    lock.unsubscribeCurrentMechanismState();
  }
}
