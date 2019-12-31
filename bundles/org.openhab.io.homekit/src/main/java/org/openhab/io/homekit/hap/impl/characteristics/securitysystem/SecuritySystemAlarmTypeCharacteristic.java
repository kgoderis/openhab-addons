package org.openhab.io.homekit.hap.impl.characteristics.securitysystem;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.SecuritySystem;
import  org.openhab.io.homekit.hap.accessories.properties.SecuritySystemAlarmType;
import  org.openhab.io.homekit.hap.characteristics.EnumCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import java.util.concurrent.CompletableFuture;

public class SecuritySystemAlarmTypeCharacteristic extends EnumCharacteristic
    implements EventableCharacteristic {

  private final SecuritySystem securitySystem;

  public SecuritySystemAlarmTypeCharacteristic(SecuritySystem securitySystem) {
    super("0000008E-0000-1000-8000-0026BB765291", false, true, "Security system alarm type", 1);
    this.securitySystem = securitySystem;
  }

  @Override
  protected CompletableFuture<Integer> getValue() {
    return securitySystem.getAlarmTypeState().thenApply(SecuritySystemAlarmType::getCode);
  }

  @Override
  protected void setValue(Integer value) throws Exception {
    // Not writable
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    securitySystem.subscribeAlarmTypeState(callback);
  }

  @Override
  public void unsubscribe() {
    securitySystem.unsubscribeAlarmTypeState();
  }
}
