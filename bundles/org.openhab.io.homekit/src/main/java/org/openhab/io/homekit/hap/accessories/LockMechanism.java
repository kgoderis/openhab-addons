package org.openhab.io.homekit.hap.accessories;

import  org.openhab.io.homekit.hap.HomekitAccessory;
import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.Service;
import  org.openhab.io.homekit.hap.accessories.properties.LockMechanismState;
import  org.openhab.io.homekit.hap.impl.services.LockMechanismService;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * A lock capable of exposing its binary locked state. For a lock that can be locked/unlocked, use
 * {@link LockableLockMechanism}.
 *
 * <p>Locks that run on batteries will need to implement this interface and also implement {@link
 * BatteryStatusAccessory}.
 *
 * @author Andy Lintner
 */
public interface LockMechanism extends HomekitAccessory {

  /**
   * Retrieves the current binary state of the lock.
   *
   * @return a future that will contain the binary state.
   */
  CompletableFuture<LockMechanismState> getCurrentMechanismState();

  /**
   * Subscribes to changes in the binary state of the lock.
   *
   * @param callback the function to call when the state changes.
   */
  void subscribeCurrentMechanismState(HomekitCharacteristicChangeCallback callback);

  /** Unsubscribes from changes in the binary state of the lock. */
  void unsubscribeCurrentMechanismState();

  @Override
  default Collection<Service> getServices() {
    return Collections.singleton(new LockMechanismService(this));
  }
}
