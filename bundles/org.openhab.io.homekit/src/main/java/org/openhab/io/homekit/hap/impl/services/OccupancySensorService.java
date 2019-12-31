package org.openhab.io.homekit.hap.impl.services;

import  org.openhab.io.homekit.hap.accessories.OccupancySensor;
import  org.openhab.io.homekit.hap.impl.characteristics.occupancysensor.OccupancyDetectedStateCharacteristic;

public class OccupancySensorService extends AbstractServiceImpl {

  public OccupancySensorService(OccupancySensor occupancySensor) {
    this(occupancySensor, occupancySensor.getLabel());
  }

  public OccupancySensorService(OccupancySensor occupancySensor, String serviceName) {
    super("00000086-0000-1000-8000-0026BB765291", occupancySensor, serviceName);
    addCharacteristic(new OccupancyDetectedStateCharacteristic(occupancySensor));
  }
}
