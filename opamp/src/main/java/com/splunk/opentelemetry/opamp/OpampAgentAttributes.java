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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.opamp.client.OpampClientBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class OpampAgentAttributes {
  private static final List<String> IDENTIFYING_ATTRIBUTES =
      Arrays.asList("service.name", "service.namespace", "service.instance.id");

  private final Resource resource;

  OpampAgentAttributes(Resource resource) {
    this.resource = resource;
  }

  void addIdentifyingAttributes(OpampClientBuilder builder) {
    resource.getAttributes().asMap().entrySet().stream()
        .filter(entry -> IDENTIFYING_ATTRIBUTES.contains(entry.getKey().getKey()))
        .forEach(putIdentifyingAttribute(builder));
  }

  void addNonIdentifyingAttributes(OpampClientBuilder builder) {
    resource.getAttributes().asMap().entrySet().stream()
        .filter(entry -> !IDENTIFYING_ATTRIBUTES.contains(entry.getKey().getKey()))
        .forEach(putNonIdentifyingAttribute(builder));
  }

  private Consumer<? super Map.Entry<AttributeKey<?>, Object>> putIdentifyingAttribute(
      OpampClientBuilder builder) {
    return entry -> {
      AttributeKey<?> key = entry.getKey();
      Object value = entry.getValue();
      AttributeType type = key.getType();

      // The java type system is truly insufferable.
      switch (type) {
        case STRING:
        case VALUE:
          builder.putIdentifyingAttribute(key.getKey(), (String) makeValue(type, value));
          break;
        case LONG:
          builder.putIdentifyingAttribute(key.getKey(), (long) makeValue(type, value));
          break;
        case DOUBLE:
          builder.putIdentifyingAttribute(key.getKey(), (double) makeValue(type, value));
          break;
        case BOOLEAN:
          builder.putIdentifyingAttribute(key.getKey(), (boolean) makeValue(type, value));
          break;
        case STRING_ARRAY:
          builder.putIdentifyingAttribute(key.getKey(), (String[]) makeValue(type, value));
          break;
        case LONG_ARRAY:
          builder.putIdentifyingAttribute(key.getKey(), (long[]) makeValue(type, value));
          break;
        case DOUBLE_ARRAY:
          builder.putIdentifyingAttribute(key.getKey(), (double[]) makeValue(type, value));
          break;
        case BOOLEAN_ARRAY:
          builder.putIdentifyingAttribute(key.getKey(), (boolean[]) makeValue(type, value));
          break;
      }
    };
  }

  private Consumer<? super Map.Entry<AttributeKey<?>, Object>> putNonIdentifyingAttribute(
      OpampClientBuilder builder) {
    return entry -> {
      AttributeKey<?> key = entry.getKey();
      Object value = entry.getValue();
      AttributeType type = key.getType();

      // The java type system is truly insufferable.
      switch (type) {
        case STRING:
        case VALUE:
          builder.putNonIdentifyingAttribute(key.getKey(), (String) makeValue(type, value));
          break;
        case LONG:
          builder.putNonIdentifyingAttribute(key.getKey(), (long) makeValue(type, value));
          break;
        case DOUBLE:
          builder.putNonIdentifyingAttribute(key.getKey(), (double) makeValue(type, value));
          break;
        case BOOLEAN:
          builder.putNonIdentifyingAttribute(key.getKey(), (boolean) makeValue(type, value));
          break;
        case STRING_ARRAY:
          builder.putNonIdentifyingAttribute(key.getKey(), (String[]) makeValue(type, value));
          break;
        case LONG_ARRAY:
          builder.putNonIdentifyingAttribute(key.getKey(), (long[]) makeValue(type, value));
          break;
        case DOUBLE_ARRAY:
          builder.putNonIdentifyingAttribute(key.getKey(), (double[]) makeValue(type, value));
          break;
        case BOOLEAN_ARRAY:
          builder.putNonIdentifyingAttribute(key.getKey(), (boolean[]) makeValue(type, value));
          break;
      }
    };
  }

  private Object makeValue(AttributeType attrType, Object value) {
    // More java type insanity
    switch (attrType) {
      case STRING:
      case LONG:
      case DOUBLE:
      case BOOLEAN:
        return value;
      case VALUE:
        return value.toString();
      case STRING_ARRAY:
        List<String> typedValueList = (List<String>) value;
        return typedValueList.toArray(new String[] {});
      case LONG_ARRAY:
        List<Long> longList = (List<Long>) value;
        long[] longArray = new long[longList.size()];
        for (int i = 0; i < longList.size(); i++) {
          longArray[i] = longList.get(i);
        }
        return longArray;
      case DOUBLE_ARRAY:
        List<Double> doubleList = (List<Double>) value;
        double[] doubleArray = new double[doubleList.size()];
        for (int i = 0; i < doubleList.size(); i++) {
          doubleArray[i] = doubleList.get(i);
        }
        return doubleArray;
      case BOOLEAN_ARRAY:
        List<Boolean> booleanList = (List<Boolean>) value;
        boolean[] booleanArray = new boolean[booleanList.size()];
        for (int i = 0; i < booleanList.size(); i++) {
          booleanArray[i] = booleanList.get(i);
        }
        return booleanArray;
    }
    return null;
  }
}
