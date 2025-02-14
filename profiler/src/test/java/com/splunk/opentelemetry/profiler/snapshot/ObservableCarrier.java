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

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Test-only "Carrier" that also implements of both the {@link TextMapGetter} and {@link
 * TextMapSetter} interfaces. Intended for use with {@link
 * io.opentelemetry.context.propagation.TextMapPropagator} implementations.
 *
 * <p>Allows for getting and setting values either directly or indirectly through the OpenTelemetry
 * {@link TextMapGetter} and {@link TextMapSetter} interfaces.
 */
class ObservableCarrier
    implements TextMapSetter<ObservableCarrier>,
        TextMapGetter<ObservableCarrier>,
        AfterEachCallback {
  private final Map<String, String> fields = new HashMap<>();

  @Override
  public Iterable<String> keys(ObservableCarrier carrier) {
    return carrier.keys();
  }

  public Set<String> keys() {
    return fields.keySet();
  }

  @Override
  public String get(ObservableCarrier carrier, String key) {
    if (carrier == null) {
      return null;
    }
    return carrier.get(key);
  }

  public String get(String key) {
    return fields.get(key);
  }

  @Override
  public void set(ObservableCarrier carrier, String key, String value) {
    if (carrier != null) {
      carrier.set(key, value);
    }
  }

  public void set(String key, String value) {
    fields.put(key, value);
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    fields.clear();
  }
}
