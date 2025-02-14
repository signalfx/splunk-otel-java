/*
 * 2024 Copyright (C) AppDynamics, Inc., and its affiliates
 * All Rights Reserved
 */

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.splunk.opentelemetry.profiler.snapshot.simulation;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Message implements Map<String, String> {
  private final Map<String, String> map = new ConcurrentHashMap<>();

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public String get(Object key) {
    return map.get(key);
  }

  @Override
  public String put(String key, String value) {
    return map.put(key, value);
  }

  @Override
  public String remove(Object key) {
    return map.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ? extends String> otherMap) {
    map.putAll(otherMap);
  }

  @Override
  public void clear() {}

  @Override
  public Set<String> keySet() {
    return map.keySet();
  }

  @Override
  public Collection<String> values() {
    return map.values();
  }

  @Override
  public Set<Entry<String, String>> entrySet() {
    return map.entrySet();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Message message = (Message) o;
    return Objects.equals(map, message.map);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public String toString() {
    return map.toString();
  }
}
