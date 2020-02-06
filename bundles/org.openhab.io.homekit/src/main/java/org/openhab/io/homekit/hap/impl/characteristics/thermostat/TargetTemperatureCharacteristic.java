package org.openhab.io.homekit.hap.impl.characteristics.thermostat;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.thermostat.BasicThermostat;
import java.util.concurrent.CompletableFuture;

public class TargetTemperatureCharacteristic extends AbstractTemperatureCharacteristic {

  private final BasicThermostat thermostat;

  public TargetTemperatureCharacteristic(BasicThermostat thermostat) {
    super("00000035-0000-1000-8000-0026BB765291", true, "Target Temperature", thermostat);
    this.thermostat = thermostat;
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    thermostat.subscribeTargetTemperature(callback);
  }

  @Override
  public void unsubscribe() {
    thermostat.unsubscribeTargetTemperature();
  }

  @Override
  protected CompletableFuture<Double> getDoubleValue() {
    return thermostat.getTargetTemperature();
  }

  @Override
  protected void setValue(Double value) throws Exception {
    thermostat.setTargetTemperature(value);
  }
}
