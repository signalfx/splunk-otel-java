package io.opentelemetry.sdk.autoconfigure;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

// TODO: Move the methods to DeclarativeConfigurationCustomizerProvider
@SuppressWarnings("unchecked")
public class AdditionalPropertiesUtil {
  public static Object getAdditionalProperty(Map<String, Object> additionalProperties, String propertyName) {
    String[] propertyNameSegments = propertyName.trim().split("\\.");
    if (propertyNameSegments.length == 0) {
      throw new IllegalArgumentException("Empty property name");
    }

    Object target = additionalProperties;
    int lastSegmentIndex = propertyNameSegments.length - 1;
    if (lastSegmentIndex > 0) {
      for (int i = 0; i < lastSegmentIndex; i++) {
        target = ((Map<String, Object>)target).get(propertyNameSegments[i]);
        if (target == null) {
          return null;
        } else if (!(target instanceof Map)) {
          throw new IllegalArgumentException("Property name: " + propertyName + " inconsistent on segment " + i + " with additional properties structure: " + additionalProperties);
        }
      }
    }
    return ((Map<String, Object>)target).get(propertyNameSegments[lastSegmentIndex]);
  }

  public static Object getAdditionalPropertyOrDefault(Map<String, Object> additionalProperties, String propertyName, Object defaultValue) {
    Object value = getAdditionalProperty(additionalProperties, propertyName);
    return value == null ? defaultValue : value;
  }

  public static void setAdditionalProperty(Map<String, Object> additionalProperties, String propertyName, Object propertyValue) {
    processAdditionalProperty(additionalProperties, propertyName,
        (targetNode, targetNameSegment) -> targetNode.put(targetNameSegment, propertyValue));
  }

  public static void addAdditionalPropertyIfAbsent(Map<String, Object> additionalProperties, String propertyName, Object propertyValue) {
    processAdditionalProperty(additionalProperties, propertyName,
        (targetNode, targetNameSegment) -> targetNode.putIfAbsent(targetNameSegment, propertyValue));
  }

  private static void processAdditionalProperty(
      Map<String, Object> additionalProperties,
      String propertyName,
      BiConsumer<Map<String, Object>, String> processProperty) {
    if (propertyName.startsWith("otel.instrumentation.")) {
      propertyName = propertyName.replace("otel.instrumentation.", "");
    }
    String[] propertyNameSegments = propertyName.trim().split("\\.");
    if (propertyNameSegments.length == 0) {
      throw new IllegalArgumentException("Empty property name");
    }

    Object target = additionalProperties;
    int lastSegmentIndex = propertyNameSegments.length - 1;
    if (lastSegmentIndex > 0) {
      for (int i = 0; i < lastSegmentIndex; i++) {
        target = ((Map<String, Object>)target).computeIfAbsent(propertyNameSegments[i], k -> new LinkedHashMap<String, Object>());
        if (!(target instanceof Map)) {
          throw new IllegalArgumentException("Property name: " + propertyName + " inconsistent on segment " + i + " with additional properties structure: " + additionalProperties);
        }
      }
    }

    processProperty.accept((Map<String, Object>)target, propertyNameSegments[lastSegmentIndex]);
  }
}
