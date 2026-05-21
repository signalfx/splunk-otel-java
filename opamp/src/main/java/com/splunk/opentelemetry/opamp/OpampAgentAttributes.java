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

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_INSTANCE_ID;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAMESPACE;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_DISTRO_VERSION;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.opamp.client.OpampClientBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class OpampAgentAttributes {
  private static final List<AttributeKey<?>> SKIPPED_NONIDENTIFYING_ATTRIBUTES =
      Arrays.asList(SERVICE_NAME, SERVICE_NAMESPACE, SERVICE_INSTANCE_ID, TELEMETRY_DISTRO_VERSION);

  private final Resource resource;

  OpampAgentAttributes(Resource resource) {
    this.resource = resource;
  }

  void addIdentifyingAttributes(OpampClientBuilder builder) {
    putIdentifyingAttribute(builder, SERVICE_NAME);
    putIdentifyingAttribute(builder, SERVICE_NAMESPACE);
    putIdentifyingAttribute(builder, SERVICE_INSTANCE_ID);
    // An agent version must be reported as a service version in identifying attributes
    putIdentifyingAttribute(builder, TELEMETRY_DISTRO_VERSION, SERVICE_VERSION.getKey());
  }

  void addNonIdentifyingAttributes(OpampClientBuilder builder) {
    resource.getAttributes().asMap().entrySet().stream()
        .filter(entry -> !SKIPPED_NONIDENTIFYING_ATTRIBUTES.contains(entry.getKey()))
        .forEach(putNonIdentifyingAttribute(builder));
  }

  private void putIdentifyingAttribute(OpampClientBuilder builder, AttributeKey<?> attributeKey) {
    putIdentifyingAttribute(builder, attributeKey, attributeKey.getKey());
  }

  private void putIdentifyingAttribute(
      OpampClientBuilder builder, AttributeKey<?> sourceAttributeKey, String targetAttributeName) {
    Object value = resource.getAttribute(sourceAttributeKey);
    if (value == null) {
      return;
    }
    AttributeType type = sourceAttributeKey.getType();

    switch (type) {
      case VALUE:
        builder.putIdentifyingAttribute(targetAttributeName, value.toString());
        break;
      case STRING:
        builder.putIdentifyingAttribute(targetAttributeName, (String) value);
        break;
      case LONG:
        builder.putIdentifyingAttribute(targetAttributeName, (long) value);
        break;
      case DOUBLE:
        builder.putIdentifyingAttribute(targetAttributeName, (double) value);
        break;
      case BOOLEAN:
        builder.putIdentifyingAttribute(targetAttributeName, (boolean) value);
        break;
      case STRING_ARRAY:
        builder.putIdentifyingAttribute(targetAttributeName, toStringArray((List<?>) value));
        break;
      case LONG_ARRAY:
        builder.putIdentifyingAttribute(targetAttributeName, toLongArray((List<?>) value));
        break;
      case DOUBLE_ARRAY:
        builder.putIdentifyingAttribute(targetAttributeName, toDoubleArray((List<?>) value));
        break;
      case BOOLEAN_ARRAY:
        builder.putIdentifyingAttribute(targetAttributeName, toBooleanArray((List<?>) value));
        break;
    }
  }

  private Consumer<? super Map.Entry<AttributeKey<?>, Object>> putNonIdentifyingAttribute(
      OpampClientBuilder builder) {
    return entry -> {
      AttributeKey<?> key = entry.getKey();
      Object value = entry.getValue();
      if (value == null) {
        return;
      }
      AttributeType type = key.getType();

      switch (type) {
        case VALUE:
          builder.putNonIdentifyingAttribute(key.getKey(), value.toString());
          break;
        case STRING:
          builder.putNonIdentifyingAttribute(key.getKey(), (String) value);
          break;
        case LONG:
          builder.putNonIdentifyingAttribute(key.getKey(), (long) value);
          break;
        case DOUBLE:
          builder.putNonIdentifyingAttribute(key.getKey(), (double) value);
          break;
        case BOOLEAN:
          builder.putNonIdentifyingAttribute(key.getKey(), (boolean) value);
          break;
        case STRING_ARRAY:
          builder.putNonIdentifyingAttribute(key.getKey(), toStringArray((List<?>) value));
          break;
        case LONG_ARRAY:
          builder.putNonIdentifyingAttribute(key.getKey(), toLongArray((List<?>) value));
          break;
        case DOUBLE_ARRAY:
          builder.putNonIdentifyingAttribute(key.getKey(), toDoubleArray((List<?>) value));
          break;
        case BOOLEAN_ARRAY:
          builder.putNonIdentifyingAttribute(key.getKey(), toBooleanArray((List<?>) value));
          break;
      }
    };
  }

  private String[] toStringArray(List<?> value) {
    return value.stream().map(v -> (String) v).toArray(String[]::new);
  }

  private long[] toLongArray(List<?> value) {
    return value.stream().mapToLong(v -> (Long) v).toArray();
  }

  private double[] toDoubleArray(List<?> value) {
    return value.stream().mapToDouble(v -> (Double) v).toArray();
  }

  private boolean[] toBooleanArray(List<?> value) {
    boolean[] result = new boolean[value.size()];
    for (int i = 0; i < value.size(); i++) {
      result[i] = (Boolean) value.get(i);
    }
    return result;
  }
}
