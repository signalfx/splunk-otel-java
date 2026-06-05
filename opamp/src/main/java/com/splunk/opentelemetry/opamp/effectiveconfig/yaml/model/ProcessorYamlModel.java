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

class ProcessorYamlModel {
  @YamlProperty(value = "batch", order = 0)
  private ExporterHolderYamlModel batch;

  @YamlProperty(value = "simple", order = 1)
  private ExporterHolderYamlModel simple;

  static ProcessorYamlModel create(String processorType, ExporterYamlModel exporter) {
    ProcessorYamlModel processor = new ProcessorYamlModel();
    ExporterHolderYamlModel exporterHolder = new ExporterHolderYamlModel(exporter);
    if ("batch".equals(processorType)) {
      processor.batch = exporterHolder;
    } else if ("simple".equals(processorType)) {
      processor.simple = exporterHolder;
    } else {
      throw new IllegalArgumentException("Unsupported processor type: " + processorType);
    }
    return processor;
  }
}
