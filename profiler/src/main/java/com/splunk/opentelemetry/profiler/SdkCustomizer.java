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

import com.google.auto.service.AutoService;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.SdkTracerProviderConfigurer;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;

@AutoService(SdkTracerProviderConfigurer.class)
public class SdkCustomizer implements SdkTracerProviderConfigurer {

  @Override
  public void configure(SdkTracerProviderBuilder tracerProviderBuilder, ConfigProperties config) {
    if (jfrIsAvailable() && jfrIsEnabledInConfig()) {
      ContextStorage.addWrapper(JfrContextStorage::new);
    }
  }

  private boolean jfrIsAvailable() {
    return JFR.instance.isAvailable();
  }

  private boolean jfrIsEnabledInConfig() {
    return Config.get().getBoolean(CONFIG_KEY_ENABLE_PROFILER, false);
  }
}
