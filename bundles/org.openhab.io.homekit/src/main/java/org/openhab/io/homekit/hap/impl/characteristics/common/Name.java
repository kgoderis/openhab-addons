package org.openhab.io.homekit.hap.impl.characteristics.common;

import  org.openhab.io.homekit.hap.characteristics.StaticStringCharacteristic;

public class Name extends StaticStringCharacteristic {

  public Name(String label) {
    super("00000023-0000-1000-8000-0026BB765291", "Name of the accessory", label);
  }
}
