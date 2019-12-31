package org.openhab.io.homekit.hap.impl.services;

import  org.openhab.io.homekit.hap.accessories.Outlet;
import  org.openhab.io.homekit.hap.impl.characteristics.common.PowerStateCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.outlet.OutletInUseCharacteristic;

public class OutletService extends AbstractServiceImpl {

  public OutletService(Outlet outlet) {
    this(outlet, outlet.getLabel());
  }

  public OutletService(Outlet outlet, String serviceName) {
    super("00000047-0000-1000-8000-0026BB765291", outlet, serviceName);
    addCharacteristic(
        new PowerStateCharacteristic(
            () -> outlet.getPowerState(),
            v -> outlet.setPowerState(v),
            c -> outlet.subscribePowerState(c),
            () -> outlet.unsubscribePowerState()));
    addCharacteristic(new OutletInUseCharacteristic(outlet));
  }
}
