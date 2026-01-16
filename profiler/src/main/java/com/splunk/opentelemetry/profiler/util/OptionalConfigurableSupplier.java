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

import java.util.Optional;
import java.util.function.Supplier;

public class OptionalConfigurableSupplier<T> implements Supplier<T> {
  private final Optional<T> defaultValue;
  private volatile Optional<T> value;

  public OptionalConfigurableSupplier() {
    this.defaultValue = Optional.empty();
    this.value = Optional.empty();
  }

  @Override
  public T get() {
    return value.orElseThrow(() -> new IllegalStateException("No value present"));
  }

  public void configure(T value) {
    this.value = Optional.of(value);
  }

  public boolean isConfigured() {
    return value.isPresent();
  }

  public void reset() {
    this.value = Optional.empty();
  }
}
