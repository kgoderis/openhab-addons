package org.openhab.io.homekit.hap.impl.characteristics.information;

import  org.openhab.io.homekit.hap.HomekitAccessory;
import  org.openhab.io.homekit.hap.characteristics.StaticStringCharacteristic;

public class Manufacturer extends StaticStringCharacteristic {

  public Manufacturer(HomekitAccessory accessory) throws Exception {
    super(
        "00000020-0000-1000-8000-0026BB765291",
        "The name of the manufacturer",
        accessory.getManufacturer());
  }
}
