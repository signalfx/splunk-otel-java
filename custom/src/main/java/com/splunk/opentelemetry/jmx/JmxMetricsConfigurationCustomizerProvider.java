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

package com.splunk.opentelemetry.jmx;

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getDistributionConfig;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizerProvider;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class JmxMetricsConfigurationCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {
  // Captured during SDK configuration for use by the agent startup listener.
  private static volatile DeclarativeConfigProperties jmxConfig = empty();

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(model -> {
      jmxConfig =
          getDistributionConfig(model).getStructured("java", empty()).getStructured("jmx", empty());
      return model;
    });
  }

  static DeclarativeConfigProperties getJmxConfig() {
    return jmxConfig;
  }
}
