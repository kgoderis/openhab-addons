package org.openhab.io.homekit.hap.accessories;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import java.util.concurrent.CompletableFuture;

/**
 * Extends {@link Valve} with timer values.
 *
 * @author Tim Harper
 */
public interface ValveWithTimer extends Valve {

  /**
   * Retrieves the current duration for which the valve will run
   *
   * @return a future with the value
   */
  CompletableFuture<Integer> getRemainingDuration();

  /**
   * Subscribes to changes in the duration; note it is not necessary to emit a change every second,
   * homekit infers the countdown progress clientside.
   *
   * @param callback the function when the existing duration has been replaced with a new one.
   */
  void subscribeRemainingDuration(HomekitCharacteristicChangeCallback callback);

  /** Unsubscribes from changes */
  void unsubscribeRemainingDuration();

  /**
   * Retrieves the current set duration for which the valve will be scheduled to run; this is
   * usually used as the duration to use when the valve is set to active.
   *
   * @return a future with the value
   */
  CompletableFuture<Integer> getSetDuration();

  /**
   * Sets the duration for which the valve will be scheduled to run; this is usually used as the
   * duration to use when the valve is set to active.
   *
   * <p>If the valve is currently running, then Homekit assumes that changing this value affects the
   * current remaining duration.
   *
   * @return a future with the value
   */
  CompletableFuture<Void> setSetDuration(int value);

  /**
   * Subscribes to changes in the set duration
   *
   * @param callback the function when the value has changed
   */
  void subscribeSetDuration(HomekitCharacteristicChangeCallback callback);

  /** Unsubscribes from changes */
  void unsubscribeSetDuration();
}
