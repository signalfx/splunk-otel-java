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

package com.splunk.opentelemetry.opamp.effectiveconfig.yaml.model;

import com.splunk.opentelemetry.opamp.effectiveconfig.yaml.YamlProperty;

public class EffectiveConfigYamlModel {
  @YamlProperty(value = "otel_config_file", order = 0, includeNull = true)
  private String otelConfigFile;

  @YamlProperty(value = "otel_experimental_config_file", order = 1, includeNull = true)
  private String otelExperimentalConfigFile;

  @YamlProperty(value = "tracer_provider", order = 2)
  private final TracerProviderYamlModel tracerProvider = new TracerProviderYamlModel();

  @YamlProperty(value = "meter_provider", order = 3)
  private final MeterProviderYamlModel meterProvider = new MeterProviderYamlModel();

  @YamlProperty(value = "logger_provider", order = 4)
  private final LoggerProviderYamlModel loggerProvider = new LoggerProviderYamlModel();

  @YamlProperty(value = "distribution", order = 5)
  private final DistributionYamlModel distribution = new DistributionYamlModel();

  public EffectiveConfigYamlModel configFiles(
      String otelConfigFile, String otelExperimentalConfigFile) {
    this.otelConfigFile = otelConfigFile;
    this.otelExperimentalConfigFile = otelExperimentalConfigFile;
    return this;
  }

  public TracerProviderYamlModel tracerProvider() {
    return tracerProvider;
  }

  public MeterProviderYamlModel meterProvider() {
    return meterProvider;
  }

  public LoggerProviderYamlModel loggerProvider() {
    return loggerProvider;
  }

  public DistributionYamlModel distribution() {
    return distribution;
  }
}
