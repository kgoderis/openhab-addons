package org.openhab.io.homekit.hap.accessories;

import  org.openhab.io.homekit.hap.HomekitAccessory;
import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.Service;
import  org.openhab.io.homekit.hap.impl.services.LightSensorService;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * A light sensor that reports current ambient light level.
 *
 * @author Gaston Dombiak
 */
public interface LightSensor extends HomekitAccessory {

  /**
   * Retrieves the current ambient light level.
   *
   * @return a future that will contain the luminance level expressed in LUX.
   */
  CompletableFuture<Double> getCurrentAmbientLightLevel();

  @Override
  default Collection<Service> getServices() {
    return Collections.singleton(new LightSensorService(this));
  }

  /**
   * Subscribes to changes in the current ambient light level.
   *
   * @param callback the function to call when the state changes.
   */
  void subscribeCurrentAmbientLightLevel(HomekitCharacteristicChangeCallback callback);

  /** Unsubscribes from changes in the current ambient light level. */
  void unsubscribeCurrentAmbientLightLevel();
}
