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

import static java.util.Collections.emptyMap;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.SplunkConfiguration;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class SdkCustomizer implements AutoConfigurationCustomizerProvider {

  @Override
  public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
    autoConfigurationCustomizer.addPropertiesCustomizer(
        config -> {
          if (jfrIsAvailable() && jfrIsEnabledInConfig(config)) {
            ContextStorage.addWrapper(JfrContextStorage::new);
          }
          return emptyMap();
        });
  }

  private boolean jfrIsAvailable() {
    return JFR.getInstance().isAvailable();
  }

  private boolean jfrIsEnabledInConfig(ConfigProperties config) {
    return SplunkConfiguration.isProfilerEnabled(config);
  }
}
