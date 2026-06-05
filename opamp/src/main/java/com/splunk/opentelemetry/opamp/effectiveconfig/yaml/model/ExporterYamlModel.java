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

public class ExporterYamlModel {
  @YamlProperty(value = "otlp_http", order = 0)
  private EndpointExporterYamlModel otlpHttp;

  @YamlProperty(value = "otlp_grpc", order = 1)
  private EndpointExporterYamlModel otlpGrpc;

  public static ExporterYamlModel otlpHttp(String endpoint) {
    ExporterYamlModel exporter = new ExporterYamlModel();
    exporter.otlpHttp = new EndpointExporterYamlModel(endpoint);
    return exporter;
  }

  public static ExporterYamlModel otlpGrpc(String endpoint) {
    ExporterYamlModel exporter = new ExporterYamlModel();
    exporter.otlpGrpc = new EndpointExporterYamlModel(endpoint);
    return exporter;
  }
}
