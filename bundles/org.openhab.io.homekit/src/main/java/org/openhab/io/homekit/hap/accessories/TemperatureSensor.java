package org.openhab.io.homekit.hap.accessories;

import  org.openhab.io.homekit.hap.*;
import  org.openhab.io.homekit.hap.accessories.properties.TemperatureUnit;
import  org.openhab.io.homekit.hap.impl.services.TemperatureSensorService;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * A temperature sensor that reports the current temperature
 *
 * @author Andy Lintner
 */
public interface TemperatureSensor extends HomekitAccessory {

  /**
   * Retrieves the current temperature, in celsius degrees.
   *
   * @return a future that will contain the temperature.
   */
  CompletableFuture<Double> getCurrentTemperature();

  @Override
  default Collection<Service> getServices() {
    return Collections.singleton(new TemperatureSensorService(this));
  }

  /**
   * Subscribes to changes in the current temperature.
   *
   * @param callback the function to call when the state changes.
   */
  void subscribeCurrentTemperature(HomekitCharacteristicChangeCallback callback);

  /** Unsubscribes from changes in the current temperature. */
  void unsubscribeCurrentTemperature();

  /**
   * Retrieves the minimum temperature, in celsius degrees, the thermostat can be set to.
   *
   * @return the minimum temperature.
   */
  double getMinimumTemperature();

  /**
   * Retrieves the maximum temperature, in celsius degrees, the thermostat can be set to.
   *
   * @return the maximum temperature.
   */
  double getMaximumTemperature();

  /**
   * Retrieves the temperature unit of the thermostat. The impact of this is unclear, as the actual
   * temperature is always communicated in celsius degrees, and the iOS device uses the user's
   * locale to determine the unit to convert to.
   *
   * @return the temperature unit of the thermostat.
   */
  default TemperatureUnit getTemperatureUnit() {
    return TemperatureUnit.CELSIUS;
  }

  /**
   * set default temperature unit of the thermostat. this is the unit thermostat use to display
   * temprature. the homekit interface uses celsius.
   *
   * @param unit the temperature unit of the thermostat.
   */
  default void setTemperatureUnit(TemperatureUnit unit) {
    // override depending on the thermostat if required.
  }

  /**
   * subscribe to unit changes.
   *
   * @param callback callback
   */
  default void subscribeTemperatureUnit(final HomekitCharacteristicChangeCallback callback) {}

  /** unsubscribe from unit changes. */
  default void unsubscribeTemperatureUnit() {}
}
