package org.openhab.io.homekit.hap.impl.characteristics.information;

import  org.openhab.io.homekit.hap.HomekitAccessory;
import  org.openhab.io.homekit.hap.characteristics.StaticStringCharacteristic;

public class SerialNumber extends StaticStringCharacteristic {

  public SerialNumber(HomekitAccessory accessory) throws Exception {
    super(
        "00000030-0000-1000-8000-0026BB765291",
        "The serial number of the accessory",
        accessory.getSerialNumber());
  }
}
