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

package com.splunk.opentelemetry.profiler.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OptionalConfigurableSupplierTest {
  private final OptionalConfigurableSupplier<String> supplier =
      new OptionalConfigurableSupplier<>();

  @Test
  void getDefaultWhenValueNotConfigured() {
    assertThat(supplier.isConfigured()).isFalse();
  }

  @Test
  void getConfiguredValue() {
    var value = "this is a configured value";

    supplier.configure(value);

    assertThat(supplier.isConfigured()).isTrue();
    assertThat(supplier.get()).isSameAs(value);
  }

  @Test
  void doNotAllowNullValues() {
    assertThatThrownBy(() -> supplier.configure(null)).isInstanceOf(Exception.class);
  }

  @Test
  void canResetSupplier() {
    supplier.configure("this is a value");
    supplier.reset();
    assertThat(supplier.isConfigured()).isFalse();
  }
}
