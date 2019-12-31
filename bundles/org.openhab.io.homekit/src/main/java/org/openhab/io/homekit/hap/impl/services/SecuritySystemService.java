package org.openhab.io.homekit.hap.impl.services;

import  org.openhab.io.homekit.hap.accessories.SecuritySystem;
import  org.openhab.io.homekit.hap.impl.characteristics.securitysystem.CurrentSecuritySystemStateCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.securitysystem.SecuritySystemAlarmTypeCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.securitysystem.TargetSecuritySystemStateCharacteristic;

public class SecuritySystemService extends AbstractServiceImpl {

  public SecuritySystemService(SecuritySystem securitySystem) {
    this(securitySystem, securitySystem.getLabel());
  }

  public SecuritySystemService(SecuritySystem securitySystem, String serviceName) {
    super("0000007E-0000-1000-8000-0026BB765291", securitySystem, serviceName);
    addCharacteristic(new CurrentSecuritySystemStateCharacteristic(securitySystem));
    addCharacteristic(new TargetSecuritySystemStateCharacteristic(securitySystem));
    addCharacteristic(new SecuritySystemAlarmTypeCharacteristic(securitySystem));
  }
}
