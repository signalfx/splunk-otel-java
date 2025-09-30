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

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_ENABLE_PROFILER;
import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.getAdditionalPropertyOrDefault;

import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalLanguageSpecificInstrumentationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.util.Map;

public class ProfilerDeclarativeConfigUtil {
  public static boolean isProfilerEnabled(OpenTelemetryConfigurationModel model) {
    if (model.getInstrumentationDevelopment() != null) {
      ExperimentalLanguageSpecificInstrumentationModel java =
          model.getInstrumentationDevelopment().getJava();

      if (java != null) {
        Map<String, Object> javaInstrumentationProperties = java.getAdditionalProperties();
        return getAdditionalPropertyOrDefault(
            javaInstrumentationProperties, CONFIG_KEY_ENABLE_PROFILER, false);
      }
    }
    return false;
  }
}
