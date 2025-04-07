package com.splunk.opentelemetry.profiler.snapshot;

import java.util.Objects;
import java.util.function.Supplier;

class ConfigurableSupplier<T> implements Supplier<T> {
  private final T defaultValue;
  private T value;

  ConfigurableSupplier(T defaultValue) {
    this.defaultValue = Objects.requireNonNull(defaultValue);
  }

  @Override
  public T get() {
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  void configure(T value) {
    this.value = Objects.requireNonNull(value);
  }

  void reset() {
    this.value = defaultValue;
  }
}
