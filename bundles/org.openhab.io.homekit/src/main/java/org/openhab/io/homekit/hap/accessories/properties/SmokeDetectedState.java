package org.openhab.io.homekit.hap.accessories.properties;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum SmokeDetectedState {
  NOT_DETECTED(0),
  DETECTED(1);

  private static final Map<Integer, SmokeDetectedState> reverse;

  static {
    reverse =
        Arrays.stream(SmokeDetectedState.values())
            .collect(Collectors.toMap(SmokeDetectedState::getCode, t -> t));
  }

  public static SmokeDetectedState fromCode(Integer code) {
    return reverse.get(code);
  }

  private final int code;

  SmokeDetectedState(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
