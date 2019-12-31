package org.openhab.io.homekit.hap.impl.services;

import  org.openhab.io.homekit.hap.accessories.HumiditySensor;
import  org.openhab.io.homekit.hap.impl.characteristics.humiditysensor.CurrentRelativeHumidityCharacteristic;

public class HumiditySensorService extends AbstractServiceImpl {

  public HumiditySensorService(HumiditySensor sensor) {
    this(sensor, sensor.getLabel());
  }

  public HumiditySensorService(HumiditySensor sensor, String serviceName) {
    super("00000082-0000-1000-8000-0026BB765291", sensor, serviceName);
    addCharacteristic(new CurrentRelativeHumidityCharacteristic(sensor));
  }
}
