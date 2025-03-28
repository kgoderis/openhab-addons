package org.openhab.io.homekit.hap.impl.characteristics.thermostat;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.properties.TemperatureUnit;
import  org.openhab.io.homekit.hap.accessories.thermostat.BasicThermostat;
import  org.openhab.io.homekit.hap.characteristics.EnumCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import java.util.concurrent.CompletableFuture;

public class TemperatureUnitsCharacteristic extends EnumCharacteristic
    implements EventableCharacteristic {

  private final BasicThermostat thermostat;

  public TemperatureUnitsCharacteristic(BasicThermostat thermostat) {
    super("00000036-0000-1000-8000-0026BB765291", true, true, "The temperature unit", 1);
    this.thermostat = thermostat;
  }

  @Override
  protected void setValue(Integer value) throws Exception {
    thermostat.setTemperatureUnit(
        value == 1 ? TemperatureUnit.FAHRENHEIT : TemperatureUnit.CELSIUS);
  }

  @Override
  protected CompletableFuture<Integer> getValue() {
    return CompletableFuture.completedFuture(thermostat.getTemperatureUnit().getCode());
  }

  @Override
  public void subscribe(final HomekitCharacteristicChangeCallback callback) {
    thermostat.subscribeTemperatureUnit(callback);
  }

  @Override
  public void unsubscribe() {
    thermostat.unsubscribeTemperatureUnit();
  }
}
