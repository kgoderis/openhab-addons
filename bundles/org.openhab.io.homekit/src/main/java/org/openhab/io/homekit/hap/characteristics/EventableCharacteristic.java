package org.openhab.io.homekit.hap.characteristics;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;

/**
 * A characteristic that can be listened to by the connected iOS device.
 *
 * @author Andy Lintner
 */
public interface EventableCharacteristic extends Characteristic {

  /**
   * Begin listening to changes to this characteristic. When a change is made, call the provided
   * function.
   *
   * @param callback a function to call when a change is made to the characteristic value.
   */
  void subscribe(HomekitCharacteristicChangeCallback callback);

  /** Stop listening to changes to this characteristic. */
  void unsubscribe();
}
