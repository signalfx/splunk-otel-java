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

package com.splunk.opentelemetry.profiler;

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_ENABLE_PROFILER;
import static com.splunk.opentelemetry.profiler.util.ProfilerDeclarativeConfigUtil.isProfilerEnabled;
import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.addAdditionalPropertyIfAbsent;
import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.getAdditionalPropertyOrDefault;

import com.google.auto.service.AutoService;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalLanguageSpecificInstrumentationModel;
import java.util.Map;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class ProfilerConfigurationCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {

  @Override
  public int order() {
    return Integer.MAX_VALUE - 1;
  }

  @Override
  public void customize(DeclarativeConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addModelCustomizer(
        model -> {
          if (model.getInstrumentationDevelopment() != null) {
            ExperimentalLanguageSpecificInstrumentationModel javaModel =
                model.getInstrumentationDevelopment().getJava();
            if (javaModel != null) {
              Map<String, Object> javaInstrumentationProperties =
                  javaModel.getAdditionalProperties();
              if (getAdditionalPropertyOrDefault(
                  javaInstrumentationProperties, CONFIG_KEY_ENABLE_PROFILER, false)) {

                Map<String, Object> defaultProperties =
                    Configuration.declarativeConfigDefaultProperties();
                defaultProperties.forEach(
                    (name, value) -> {
                      addAdditionalPropertyIfAbsent(javaInstrumentationProperties, name, value);
                    });

                if (jfrIsAvailable() && isProfilerEnabled(model)) {
                  ContextStorage.addWrapper(JfrContextStorage::new);
                }
              }
            }
          }
          return model;
        });
  }

  private boolean jfrIsAvailable() {
    return JFR.instance.isAvailable();
  }
}
