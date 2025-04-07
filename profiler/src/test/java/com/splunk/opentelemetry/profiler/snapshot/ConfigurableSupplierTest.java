package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConfigurableSupplierTest {
  private final String defaultValue = "this is a default value";
  private final ConfigurableSupplier<String> supplier = new ConfigurableSupplier<>(defaultValue);

  @Test
  void provideDefaultValueNotConfigured() {
    assertSame(defaultValue, supplier.get());
  }

  @Test
  void providedConfiguredValue() {
    var value = "this is a configured value";
    supplier.configure(value);
    assertSame(value, supplier.get());
  }

  @Test
  void doNotAllowNullValues() {
    assertThrows(Exception.class, () -> supplier.configure(null));
  }

  @Test
  void doNotAllowCreationWithNullDefaultValue() {
    assertThrows(Exception.class, () -> new ConfigurableSupplier<>(null));
  }

  @Test
  void canResetSupplier() {
    supplier.configure("this is a value");
    supplier.reset();
    assertSame(defaultValue, supplier.get());
  }
}
