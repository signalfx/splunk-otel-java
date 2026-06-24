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

package com.splunk.opentelemetry.opamp.effectiveconfig;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.HashMap;
import java.util.Map;
import okio.ByteString;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentConfigMap;

interface EffectiveConfigFactory {
  /**
   * Build <code>AgentConfigMap</code> object containing current effective config with proper
   * content type and file name
   *
   * @return agent config map containing effective config.
   * @see AgentConfigMap
   * @see AgentConfigFile
   * @see #createEffectiveConfigContent()
   * @see #getContentType()
   * @see #getFileName()
   */
  default AgentConfigMap createEffectiveConfigMap() {
    Map<String, AgentConfigFile> configMap = new HashMap<>();

    ByteString content = new ByteString(createEffectiveConfigContent().getBytes(UTF_8));
    AgentConfigFile configFile = new AgentConfigFile(content, getContentType());
    configMap.put(getFileName(), configFile);

    return new AgentConfigMap(configMap);
  }

  /**
   * Create an appropriately formatted string containing effective config of the agent. Format and
   * content may vary depending on the agent configuration.
   *
   * @return effective config serialized to string representation
   * @see <a
   *     href="https://github.com/signalfx/gdi-specification/blob/main/specification/opamp_datamodel.md#effective-configuration">GDI
   *     Spec</a> for details
   */
  String createEffectiveConfigContent();

  /**
   * Return content type that needs to be associated with content of the effective config.
   *
   * @return content type
   * @see <a
   *     href="https://github.com/signalfx/gdi-specification/blob/main/specification/opamp_datamodel.md#effective-configuration">GDI
   *     Spec</a> for details
   */
  String getContentType();

  /**
   * Return a name of the effective config that is assigned to effective config reported to the
   * server.
   *
   * @return name of effective config
   * @see <a
   *     href="https://github.com/signalfx/gdi-specification/blob/main/specification/opamp_datamodel.md#effective-configuration">GDI
   *     Spec</a> for details
   */
  String getFileName();
}
