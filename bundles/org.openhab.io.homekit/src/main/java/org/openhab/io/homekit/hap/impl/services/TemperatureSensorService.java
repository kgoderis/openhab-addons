package org.openhab.io.homekit.hap.impl.services;

import  org.openhab.io.homekit.hap.accessories.TemperatureSensor;
import  org.openhab.io.homekit.hap.impl.characteristics.thermostat.CurrentTemperatureCharacteristic;

public class TemperatureSensorService extends AbstractServiceImpl {

  public TemperatureSensorService(TemperatureSensor sensor) {
    this(sensor, sensor.getLabel());
  }

  public TemperatureSensorService(TemperatureSensor sensor, String serviceName) {
    super("0000008A-0000-1000-8000-0026BB765291", sensor, serviceName);
    addCharacteristic(new CurrentTemperatureCharacteristic(sensor));
  }
}
