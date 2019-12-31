package org.openhab.io.homekit.hap.impl.characteristics.securitysystem;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.SecuritySystem;
import  org.openhab.io.homekit.hap.accessories.properties.CurrentSecuritySystemState;
import  org.openhab.io.homekit.hap.characteristics.EnumCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import java.util.concurrent.CompletableFuture;

public class CurrentSecuritySystemStateCharacteristic extends EnumCharacteristic
    implements EventableCharacteristic {

  private final SecuritySystem securitySystem;

  public CurrentSecuritySystemStateCharacteristic(SecuritySystem securitySystem) {
    super("00000066-0000-1000-8000-0026BB765291", false, true, "Current security system state", 4);
    this.securitySystem = securitySystem;
  }

  @Override
  protected CompletableFuture<Integer> getValue() {
    return securitySystem
        .getCurrentSecuritySystemState()
        .thenApply(CurrentSecuritySystemState::getCode);
  }

  @Override
  protected void setValue(Integer value) throws Exception {
    // Not writable
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    securitySystem.subscribeCurrentSecuritySystemState(callback);
  }

  @Override
  public void unsubscribe() {
    securitySystem.unsubscribeCurrentSecuritySystemState();
  }
}
