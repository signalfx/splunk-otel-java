/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConfigurableSupplierTest {
  private final String defaultValue = "this is a default value";
  private final ConfigurableSupplier<String> supplier = new ConfigurableSupplier<>(defaultValue);

  @Test
  void getDefaultWhenValueNotConfigured() {
    assertSame(defaultValue, supplier.get());
  }

  @Test
  void getConfiguredValue() {
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
