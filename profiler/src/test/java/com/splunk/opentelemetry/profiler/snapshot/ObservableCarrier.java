/*
 * 2024 Copyright (C) AppDynamics, Inc., and its affiliates
 * All Rights Reserved
 */

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
