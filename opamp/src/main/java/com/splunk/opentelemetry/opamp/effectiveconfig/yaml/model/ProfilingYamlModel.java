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
import java.util.function.Consumer;

public class ProfilingYamlModel {
  @YamlProperty(value = "always_on", order = 0)
  private AlwaysOnYamlModel alwaysOn;

  @YamlProperty(value = "callgraphs", order = 1)
  private CallgraphsYamlModel callgraphs;

  public void alwaysOn(Consumer<AlwaysOnYamlModel> initializer) {
    AlwaysOnYamlModel alwaysOn = new AlwaysOnYamlModel();
    initializer.accept(alwaysOn);
    this.alwaysOn = alwaysOn;
  }

  public void callgraphs(long samplingInterval) {
    callgraphs = new CallgraphsYamlModel(samplingInterval);
  }
}
