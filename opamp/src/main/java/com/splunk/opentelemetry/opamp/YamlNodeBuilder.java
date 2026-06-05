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

package com.splunk.opentelemetry.opamp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class YamlNodeBuilder {
  private final Map<String, Object> map = new LinkedHashMap<>();

  private YamlNodeBuilder() {}

  public static YamlNodeBuilder create() {
    return new YamlNodeBuilder();
  }

  public YamlNodeBuilder addNode(String name, Object value) {
    if (name.trim().isEmpty()) {
      throw new IllegalArgumentException("Empty node name");
    }
    if (map.containsKey(name)) {
      throw new IllegalStateException("Node " + name + " already present");
    }

    // Additional argument check to avoid scenario when build() method is not called before add.
    // This may happen when building complex trees.
    if (value instanceof YamlNodeBuilder) {
      throw new IllegalArgumentException("Cannot add a builder as \"" + name + "\" node");
    }

    map.put(name, value);
    return this;
  }

  public YamlNodeBuilder addNestedNode(String name, Object value) {
    if (name.startsWith(".") || name.endsWith(".")) {
      throw new IllegalArgumentException("Invalid nested node name format: " + name);
    }
    int separatorIndex = name.indexOf('.');

    if (separatorIndex == -1) {
      return addNode(name, value);
    } else {
      String currentNameSegment = name.substring(0, separatorIndex);
      String remainingNameSegments = name.substring(separatorIndex + 1);
      return addNode(currentNameSegment, createNestedNode(remainingNameSegments, value));
    }
  }

  public YamlNodeBuilder addNestedNode(String name, Supplier<Object> value) {
    return addNestedNode(name, value.get());
  }

  public Map<String, Object> build() {
    return map;
  }

  public static Map<String, Object> createNode(String key, Object value) {
    return create().addNode(key, value).build();
  }

  public static Map<String, Object> createNestedNode(String key, Object value) {
    return create().addNestedNode(key, value).build();
  }
}
