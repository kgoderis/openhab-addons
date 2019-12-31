package org.openhab.io.homekit.hap.impl.services;

import  org.openhab.io.homekit.hap.accessories.GarageDoor;
import  org.openhab.io.homekit.hap.impl.characteristics.common.ObstructionDetectedCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.garage.CurrentDoorStateCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.garage.TargetDoorStateCharacteristic;

public class GarageDoorService extends AbstractServiceImpl {

  public GarageDoorService(GarageDoor door) {
    this(door, door.getLabel());
  }

  public GarageDoorService(GarageDoor door, String serviceName) {
    super("00000041-0000-1000-8000-0026BB765291", door, serviceName);
    addCharacteristic(new CurrentDoorStateCharacteristic(door));
    addCharacteristic(new TargetDoorStateCharacteristic(door));
    addCharacteristic(
        new ObstructionDetectedCharacteristic(
            () -> door.getObstructionDetected(),
            c -> door.subscribeObstructionDetected(c),
            () -> door.unsubscribeObstructionDetected()));
  }
}
