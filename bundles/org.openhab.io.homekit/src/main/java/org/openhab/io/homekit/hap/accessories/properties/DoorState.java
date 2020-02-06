package org.openhab.io.homekit.hap.accessories.properties;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum DoorState {
  OPEN(0),
  CLOSED(1),
  OPENING(2),
  CLOSING(3),
  STOPPED(4);

  private static final Map<Integer, DoorState> reverse;

  static {
    reverse = Arrays.stream(DoorState.values()).collect(Collectors.toMap(t -> t.getCode(), t -> t));
  }

  public static DoorState fromCode(Integer code) {
    return reverse.get(code);
  }

  private final int code;

  private DoorState(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
