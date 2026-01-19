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

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;

/**
 * Purpose of this class is to configure the supplier of ProfilerDeclarativeConfiguration.
 * ProfilerDeclarativeConfiguration class object can then be used in code executed after SDK is
 * created, such as AgentListeners.
 */
@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class ProfilerDeclarativeConfigurationInitializer
    implements DeclarativeConfigurationCustomizerProvider {
  public void customize(DeclarativeConfigurationCustomizer configurationCustomizer) {
    configurationCustomizer.addModelCustomizer(
        (model) -> {
          DeclarativeConfigProperties distributionConfig =
              AutoConfigureUtil.getDistributionConfig(model);
          DeclarativeConfigProperties profilingConfig =
              distributionConfig.getStructured("profiling", empty());

          ProfilerDeclarativeConfiguration.SUPPLIER.configure(
              new ProfilerDeclarativeConfiguration(profilingConfig));

          return model;
        });
  }
}
