package org.openhab.io.homekit.hap.impl.characteristics.lock.mechanism;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.LockableLockMechanism;
import  org.openhab.io.homekit.hap.accessories.properties.LockMechanismState;
import  org.openhab.io.homekit.hap.characteristics.EnumCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import java.util.concurrent.CompletableFuture;

public class TargetLockMechanismStateCharacteristic extends EnumCharacteristic
    implements EventableCharacteristic {

  private final LockableLockMechanism lock;

  public TargetLockMechanismStateCharacteristic(LockableLockMechanism lock) {
    super("0000001E-0000-1000-8000-0026BB765291", true, true, "Current lock state", 3);
    this.lock = lock;
  }

  @Override
  protected void setValue(Integer value) throws Exception {
    lock.setTargetMechanismState(LockMechanismState.fromCode(value));
  }

  @Override
  protected CompletableFuture<Integer> getValue() {
    return lock.getTargetMechanismState().thenApply(s -> s.getCode());
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    lock.subscribeTargetMechanismState(callback);
  }

  @Override
  public void unsubscribe() {
    lock.unsubscribeTargetMechanismState();
  }
}
