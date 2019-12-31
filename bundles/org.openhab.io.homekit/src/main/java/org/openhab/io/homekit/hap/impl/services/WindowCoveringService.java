package org.openhab.io.homekit.hap.impl.services;

import  org.openhab.io.homekit.hap.accessories.BasicWindowCovering;
import  org.openhab.io.homekit.hap.accessories.HoldPositionWindowCovering;
import  org.openhab.io.homekit.hap.accessories.HorizontalTiltingWindowCovering;
import  org.openhab.io.homekit.hap.accessories.ObstructionDetectedWindowCovering;
import  org.openhab.io.homekit.hap.accessories.VerticalTiltingWindowCovering;
import  org.openhab.io.homekit.hap.impl.characteristics.common.ObstructionDetectedCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.windowcovering.CurrentHorizontalTiltAngleCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.windowcovering.CurrentPositionCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.windowcovering.CurrentVerticalTiltAngleCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.windowcovering.HoldPositionCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.windowcovering.PositionStateCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.windowcovering.TargetHorizontalTiltAngleCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.windowcovering.TargetPositionCharacteristic;
import  org.openhab.io.homekit.hap.impl.characteristics.windowcovering.TargetVerticalTiltAngleCharacteristic;

public class WindowCoveringService extends AbstractServiceImpl {

  public WindowCoveringService(BasicWindowCovering windowCovering) {
    this(windowCovering, windowCovering.getLabel());
  }

  public WindowCoveringService(BasicWindowCovering windowCovering, String serviceName) {
    super("0000008C-0000-1000-8000-0026BB765291", windowCovering, serviceName);
    addCharacteristic(new CurrentPositionCharacteristic(windowCovering));
    addCharacteristic(new PositionStateCharacteristic(windowCovering));
    addCharacteristic(new TargetPositionCharacteristic(windowCovering));

    if (windowCovering instanceof HorizontalTiltingWindowCovering) {
      addCharacteristic(
          new CurrentHorizontalTiltAngleCharacteristic(
              (HorizontalTiltingWindowCovering) windowCovering));
      addCharacteristic(
          new TargetHorizontalTiltAngleCharacteristic(
              (HorizontalTiltingWindowCovering) windowCovering));
    }
    if (windowCovering instanceof VerticalTiltingWindowCovering) {
      addCharacteristic(
          new CurrentVerticalTiltAngleCharacteristic(
              (VerticalTiltingWindowCovering) windowCovering));
      addCharacteristic(
          new TargetVerticalTiltAngleCharacteristic(
              (VerticalTiltingWindowCovering) windowCovering));
    }
    if (windowCovering instanceof HoldPositionWindowCovering) {
      HoldPositionWindowCovering hpwc = (HoldPositionWindowCovering) windowCovering;
      addCharacteristic(new HoldPositionCharacteristic(hpwc));
    }
    if (windowCovering instanceof ObstructionDetectedWindowCovering) {
      ObstructionDetectedWindowCovering wc = (ObstructionDetectedWindowCovering) windowCovering;
      addCharacteristic(
          new ObstructionDetectedCharacteristic(
              () -> wc.getObstructionDetected(),
              c -> wc.subscribeObstructionDetected(c),
              () -> wc.unsubscribeObstructionDetected()));
    }
  }
}
