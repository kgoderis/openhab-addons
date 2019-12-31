package org.openhab.io.homekit.hap.impl.characteristics.thermostat;

import  org.openhab.io.homekit.hap.accessories.TemperatureSensor;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.FloatCharacteristic;

public abstract class AbstractTemperatureCharacteristic extends FloatCharacteristic
    implements EventableCharacteristic {

  public AbstractTemperatureCharacteristic(
      String type, boolean isWritable, String description, TemperatureSensor sensor) {
    super(
        type,
        isWritable,
        true,
        description,
        sensor.getMinimumTemperature(),
        sensor.getMaximumTemperature(),
        0.1,
        "celsius");
  }
}
