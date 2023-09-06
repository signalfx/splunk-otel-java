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

package com.splunk.opentelemetry;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Predicate;

class HecTelemetryInspector {
  private static final String EVENT_KEY = "event";
  private static final String FIELDS_KEY = "event";

  static Predicate<JsonNode> hasEventName(String eventName) {
    return it -> {
      JsonNode node = it.findValue(EVENT_KEY);
      return node != null && node.isTextual() && eventName.equals(node.textValue());
    };
  }

  static Predicate<JsonNode> hasTextFieldValue(String fieldName, String fieldValue) {
    return it -> {
      JsonNode node = it.findPath(FIELDS_KEY).findPath(fieldName);
      return !node.isMissingNode() && node.isTextual() && fieldValue.equals(node.textValue());
    };
  }
}
