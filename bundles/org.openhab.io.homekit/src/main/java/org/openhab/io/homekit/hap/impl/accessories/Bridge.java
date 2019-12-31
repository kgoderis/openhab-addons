package org.openhab.io.homekit.hap.impl.accessories;

import  org.openhab.io.homekit.hap.HomekitAccessory;

public interface Bridge extends HomekitAccessory {

  @Override
  default void identify() {}
}
