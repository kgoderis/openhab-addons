package org.openhab.io.homekit.hap.accessories.properties;

import  org.openhab.io.homekit.hap.accessories.SecuritySystem;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The target state of a {@link SecuritySystem}.
 *
 * @author Gaston Dombiak
 */
public enum TargetSecuritySystemState {

  /** Arm the system when the home is occupied and residents are active. */
  STAY_ARM(0),
  /** Arm the system when the home is unoccupied. */
  AWAY_ARM(1),
  /** Arm the system when the home is occupied and residents are sleeping. */
  NIGHT_ARM(2),
  /** Disarm the system. */
  DISARM(3);

  private static final Map<Integer, TargetSecuritySystemState> reverse;

  static {
    reverse =
        Arrays.stream(TargetSecuritySystemState.values())
            .collect(Collectors.toMap(TargetSecuritySystemState::getCode, t -> t));
  }

  public static TargetSecuritySystemState fromCode(Integer code) {
    return reverse.get(code);
  }

  private final int code;

  TargetSecuritySystemState(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
