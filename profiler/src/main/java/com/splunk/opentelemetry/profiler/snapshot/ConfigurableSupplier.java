package com.splunk.opentelemetry.profiler.snapshot;

import java.util.Objects;
import java.util.function.Supplier;

class ConfigurableSupplier<T> implements Supplier<T> {
  private T value;

  ConfigurableSupplier(T defaultValue) {
    this.value = defaultValue;
  }

  @Override
  public T get() {
    return value;
  }

  void configure(T value) {
    this.value = Objects.requireNonNull(value);
  }
}
