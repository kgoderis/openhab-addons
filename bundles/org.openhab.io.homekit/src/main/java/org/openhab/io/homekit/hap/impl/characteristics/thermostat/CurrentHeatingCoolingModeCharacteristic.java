package org.openhab.io.homekit.hap.impl.characteristics.thermostat;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.properties.ThermostatMode;
import  org.openhab.io.homekit.hap.accessories.thermostat.BasicThermostat;
import java.util.concurrent.CompletableFuture;

public class CurrentHeatingCoolingModeCharacteristic
    extends AbstractHeatingCoolingModeCharacteristic {

  private final BasicThermostat thermostat;

  public CurrentHeatingCoolingModeCharacteristic(BasicThermostat thermostat) {
    super("0000000F-0000-1000-8000-0026BB765291", false, "Current Mode");
    this.thermostat = thermostat;
  }

  @Override
  protected void setModeValue(ThermostatMode mode) throws Exception {
    // Not writable
  }

  @Override
  protected CompletableFuture<ThermostatMode> getModeValue() {
    return thermostat.getCurrentMode();
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    thermostat.subscribeCurrentMode(callback);
  }

  @Override
  public void unsubscribe() {
    thermostat.unsubscribeCurrentMode();
  }
}
