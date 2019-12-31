package org.openhab.io.homekit.hap.impl;

public interface ExceptionalConsumer<T> {
  void accept(T t) throws Exception;
}
