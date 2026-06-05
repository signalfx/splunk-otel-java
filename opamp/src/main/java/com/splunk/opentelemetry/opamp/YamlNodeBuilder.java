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
import java.util.function.Consumer;

public class YamlNodeBuilder {
  private final Map<String, Object> map = new LinkedHashMap<>();

  private YamlNodeBuilder() {}

  public static YamlNodeBuilder create() {
    return new YamlNodeBuilder();
  }

  /**
   * Add a new child node with the provided value.
   *
   * <p>The value can be a scalar, list, map, {@code null}, or any other value supported by the YAML
   * serializer. Pass a built map instead of a {@link YamlNodeBuilder}; builders are rejected to
   * catch accidental missing {@link #build()} calls.
   *
   * @param name node name
   * @param value a value of newly added child node
   * @return <code>this</code>
   * @throws IllegalArgumentException if provided node name is empty
   * @throws IllegalArgumentException if {@code value} is a {@link YamlNodeBuilder}
   * @throws IllegalStateException if child node with provided name already exists
   */
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

  /**
   * Add a new map child node and initialize it with a builder.
   *
   * <p>The {@code childNodeInitializer} receives a builder for the new child node. If the
   * initializer does not add any nodes, nothing is added to this builder.
   *
   * @param name node name
   * @param childNodeInitializer callback used to populate the child node
   * @return <code>this</code>
   * @throws IllegalArgumentException if provided node name is empty
   * @throws IllegalStateException if child node with provided name already exists
   */
  public YamlNodeBuilder addNode(String name, Consumer<YamlNodeBuilder> childNodeInitializer) {
    Map<String, Object> node = buildNode(childNodeInitializer);
    if (node.isEmpty()) {
      return this;
    }
    return addNode(name, node);
  }

  /**
   * Add a chain of nested child nodes. The name provided is split using '.' as a separator. The
   * deepest child node gets provided value.
   *
   * @param name dot separated chain of nested nodes names
   * @param value a value of a last child node
   * @return <code>this</code>
   * @see #addNestedNode(String, Consumer)
   */
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

  /**
   * Add a chain of nested child nodes and initialize the deepest child node with a builder passed
   * as a lambda argument.
   *
   * <p>For example, {@code addNestedNode("distribution.splunk.profiling", builder -> ...)} creates
   * the {@code distribution}, {@code splunk}, and {@code profiling} nodes, then passes a builder
   * for {@code profiling} to {@code childNodeInitializer}. If the initializer does not add any
   * nodes, nothing is added to this builder.
   *
   * @param name dot separated chain of nested nodes names
   * @param childNodeInitializer callback used to populate the last child node
   * @return <code>this</code>
   * @see #addNestedNode(String, Object)
   */
  public YamlNodeBuilder addNestedNode(
      String name, Consumer<YamlNodeBuilder> childNodeInitializer) {
    Map<String, Object> node = buildNode(childNodeInitializer);
    if (node.isEmpty()) {
      return this;
    }
    return addNestedNode(name, node);
  }

  public Map<String, Object> build() {
    return map;
  }

  /**
   * Create a map containing a single child node.
   *
   * <p>This is a convenience wrapper for {@code YamlNodeBuilder.create().addNode(key,
   * value).build()}.
   *
   * @param key node name
   * @param value a value of the node
   * @return map containing the created node
   * @see #addNode(String, Object)
   */
  public static Map<String, Object> createNode(String key, Object value) {
    return create().addNode(key, value).build();
  }

  /**
   * Create a map containing a single chain of nested child nodes.
   *
   * <p>The {@code key} is split using '.' as a separator. The deepest child node gets the provided
   * value. This is a convenience wrapper for {@code YamlNodeBuilder.create().addNestedNode(key,
   * value).build()}.
   *
   * @param key dot separated chain of nested nodes names
   * @param value a value of the last child node
   * @return map containing the created nested node chain
   * @see #addNestedNode(String, Object)
   */
  public static Map<String, Object> createNestedNode(String key, Object value) {
    return create().addNestedNode(key, value).build();
  }

  private static Map<String, Object> buildNode(Consumer<YamlNodeBuilder> childNodeInitializer) {
    YamlNodeBuilder builder = create();
    childNodeInitializer.accept(builder);
    return builder.build();
  }
}
