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

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.profiler.util.DeclarativeConfigPropertiesUtil;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import java.util.Collections;

/**
 * Purpose of this class is to configure the supplier of ProfilerConfiguration. The configured
 * object can then be used in code executed after SDK is created, such as AgentListeners.
 */
@AutoService({
  DeclarativeConfigurationCustomizerProvider.class,
  AutoConfigurationCustomizerProvider.class
})
public class ProfilerConfigurationInitializer
    implements DeclarativeConfigurationCustomizerProvider, AutoConfigurationCustomizerProvider {

  @Override
  public void customize(DeclarativeConfigurationCustomizer configurationCustomizer) {
    // Initialize profiler configuration from declarative config
    configurationCustomizer.addModelCustomizer(
        (model) -> {
          DeclarativeConfigProperties distributionConfig =
              AutoConfigureUtil.getDistributionConfig(model);
          DeclarativeConfigProperties profilingConfig =
              DeclarativeConfigPropertiesUtil.getStructuredOrEmpty(distributionConfig, "profiling");

          ProfilerConfiguration.SUPPLIER.configure(
              ProfilerDeclarativeConfigurationFactory.create(profilingConfig));

          return model;
        });
  }

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    // Initialize profiler configuration from environment config
    autoConfiguration.addPropertiesCustomizer(
        configProperties -> {
          ProfilerConfiguration.SUPPLIER.configure(
              ProfilerEnvVarsConfigurationFactory.create(configProperties));
          return Collections.emptyMap();
        });
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE;
  }
}
