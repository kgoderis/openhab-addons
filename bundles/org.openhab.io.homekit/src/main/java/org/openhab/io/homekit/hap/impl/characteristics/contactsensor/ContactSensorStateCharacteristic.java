package org.openhab.io.homekit.hap.impl.characteristics.contactsensor;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.ContactSensor;
import  org.openhab.io.homekit.hap.accessories.properties.ContactState;
import  org.openhab.io.homekit.hap.characteristics.EnumCharacteristic;
import  org.openhab.io.homekit.hap.characteristics.EventableCharacteristic;
import java.util.concurrent.CompletableFuture;

public class ContactSensorStateCharacteristic extends EnumCharacteristic
    implements EventableCharacteristic {

  private final ContactSensor contactSensor;

  public ContactSensorStateCharacteristic(ContactSensor contactSensor) {
    super("0000006A-0000-1000-8000-0026BB765291", false, true, "Contact State", 1);
    this.contactSensor = contactSensor;
  }

  @Override
  protected CompletableFuture<Integer> getValue() {
    return contactSensor.getCurrentState().thenApply(ContactState::getCode);
  }

  @Override
  protected void setValue(Integer value) throws Exception {
    // Read Only
  }

  @Override
  public void subscribe(HomekitCharacteristicChangeCallback callback) {
    contactSensor.subscribeContactState(callback);
  }

  @Override
  public void unsubscribe() {
    contactSensor.unsubscribeContactState();
  }
}
