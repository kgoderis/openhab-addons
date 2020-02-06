package org.openhab.io.homekit.hap.accessories;

import  org.openhab.io.homekit.hap.HomekitAccessory;
import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.Service;
import  org.openhab.io.homekit.hap.impl.services.OccupancySensorService;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * An occupancy sensor that reports whether occupancy has been detected.
 *
 * <p>Occupancy sensors that run on batteries will need to implement this interface and also
 * implement {@link BatteryStatusAccessory}.
 *
 * @author Tim Harper
 */
public interface OccupancySensor extends HomekitAccessory {
  /**
   * Retrieves the state of the occupancy sensor. If true then occupancy has been detected.
   *
   * @return a future that will contain the occupancy sensor's state
   */
  CompletableFuture<Boolean> getOccupancyDetected();

  @Override
  default Collection<Service> getServices() {
    return Collections.singleton(new OccupancySensorService(this));
  }

  /**
   * Subscribes to changes in the occupancy sensor.
   *
   * @param callback the function to call when the state changes.
   */
  void subscribeOccupancyDetected(HomekitCharacteristicChangeCallback callback);

  /** Unsubscribes from changes in the occupancy sensor. */
  void unsubscribeOccupancyDetected();
}
