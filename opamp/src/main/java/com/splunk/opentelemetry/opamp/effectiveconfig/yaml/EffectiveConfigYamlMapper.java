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

package com.splunk.opentelemetry.opamp.effectiveconfig.yaml;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EffectiveConfigYamlMapper {
  private EffectiveConfigYamlMapper() {}

  public static Map<String, Object> toYamlMap(Object model) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Field field : yamlFields(model.getClass())) {
      YamlProperty property = field.getAnnotation(YamlProperty.class);
      Object value = fieldValue(field, model);
      Object yamlValue = toYamlValue(value);
      if (yamlValue != null || property.includeNull()) {
        addYamlValue(result, property.value(), yamlValue);
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static void addYamlValue(Map<String, Object> result, String propertyPath, Object value) {
    String[] path = propertyPath.split("\\.");
    Map<String, Object> target = result;
    for (int i = 0; i < path.length - 1; i++) {
      Object child = target.get(path[i]);
      if (child == null) {
        child = new LinkedHashMap<String, Object>();
        target.put(path[i], child);
      }
      if (!(child instanceof Map)) {
        throw new IllegalArgumentException(
            "YAML property path conflicts with scalar: " + propertyPath);
      }
      target = (Map<String, Object>) child;
    }
    target.put(path[path.length - 1], value);
  }

  private static Object toYamlValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value.getClass().isAnnotationPresent(YamlEmptyNode.class)) {
      return EmptyYamlNode.INSTANCE;
    }
    if (value instanceof String || value instanceof Number || value instanceof Boolean) {
      return value;
    }
    if (value instanceof List) {
      return toYamlList((List<?>) value);
    }
    if (hasYamlFields(value.getClass())) {
      Map<String, Object> map = toYamlMap(value);
      return map.isEmpty() ? null : map;
    }
    throw new IllegalArgumentException("Unsupported YAML model value: " + value.getClass());
  }

  private static List<Object> toYamlList(List<?> values) {
    List<Object> result = new ArrayList<>(values.size());
    for (Object value : values) {
      Object yamlValue = toYamlValue(value);
      if (yamlValue != null) {
        result.add(yamlValue);
      }
    }
    return result.isEmpty() ? null : result;
  }

  private static List<Field> yamlFields(Class<?> type) {
    List<Field> result = new ArrayList<>();
    for (Field field : type.getDeclaredFields()) {
      if (field.isAnnotationPresent(YamlProperty.class)) {
        result.add(field);
      }
    }
    result.sort(Comparator.comparingInt(field -> field.getAnnotation(YamlProperty.class).order()));
    return result;
  }

  private static boolean hasYamlFields(Class<?> type) {
    return !yamlFields(type).isEmpty();
  }

  private static Object fieldValue(Field field, Object model) {
    try {
      field.setAccessible(true);
      return field.get(model);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Cannot read YAML model field " + field, e);
    }
  }
}
