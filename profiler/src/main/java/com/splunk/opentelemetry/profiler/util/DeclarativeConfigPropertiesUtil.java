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

package com.splunk.opentelemetry.profiler.util;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import java.util.Collections;
import java.util.Map;

public final class DeclarativeConfigPropertiesUtil {
  private DeclarativeConfigPropertiesUtil() {}

  public static DeclarativeConfigProperties getStructuredOrEmpty(
      DeclarativeConfigProperties parent, String name) {
    DeclarativeConfigProperties structured = parent.getStructured(name);
    if (structured != null) {
      return structured;
    }
    return emptyPreservingLoader(parent);
  }

  public static DeclarativeConfigProperties emptyPreservingLoader(
      DeclarativeConfigProperties parent) {
    return createPreservingLoader(parent, Collections.emptyMap());
  }

  public static DeclarativeConfigProperties createPreservingLoader(
      DeclarativeConfigProperties parent, Map<String, Object> properties) {
    return YamlDeclarativeConfigProperties.create(properties, parent.getComponentLoader());
  }
}
