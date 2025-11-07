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

import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.getAdditionalPropertyOrDefault;

import com.splunk.opentelemetry.SplunkConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalLanguageSpecificInstrumentationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.util.Map;

public class ProfilerDeclarativeConfigUtil {
  public static boolean isProfilerEnabled(OpenTelemetryConfigurationModel model) {
    return getBoolean(model, SplunkConfiguration.PROFILER_ENABLED_PROPERTY);
  }

  private static boolean getBoolean(OpenTelemetryConfigurationModel model, String propertyName) {
    if (model.getInstrumentationDevelopment() != null) {
      ExperimentalLanguageSpecificInstrumentationModel java =
          model.getInstrumentationDevelopment().getJava();

      if (java != null) {
        Map<String, Object> javaInstrumentationProperties = java.getAdditionalProperties();
        return getBoolean(javaInstrumentationProperties, propertyName);
      }
    }
    return false;
  }

  private static boolean getBoolean(
      Map<String, Object> javaInstrumentationProperties, String propertyName) {
    return getAdditionalPropertyOrDefault(javaInstrumentationProperties, propertyName, false);
  }
}
