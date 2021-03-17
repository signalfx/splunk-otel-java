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

package com.splunk.opentelemetry.helper;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractTestContainerManager implements TestContainerManager {
  protected static final int TARGET_PORT = 8080;
  protected static final int BACKEND_PORT = 8080;

  protected static final String BACKEND_ALIAS = "backend";
  protected static final String COLLECTOR_ALIAS = "collector";
  protected static final String TARGET_AGENT_FILENAME = "opentelemetry-javaagent.jar";
  protected static final String COLLECTOR_CONFIG_RESOURCE = "/otel.yaml";

  protected Map<String, String> getAgentEnvironment() {
    Map<String, String> environment = new HashMap<>();
    environment.put("JAVA_TOOL_OPTIONS", "-javaagent:/" + TARGET_AGENT_FILENAME);
    environment.put("OTEL_BSP_MAX_EXPORT_BATCH_SIZE", "1");
    environment.put("OTEL_BSP_SCHEDULE_DELAY", "10");
    environment.put("OTEL_IMR_EXPORT_INTERVAL", "1000");
    environment.put(
        "OTEL_EXPORTER_JAEGER_ENDPOINT", "http://" + COLLECTOR_ALIAS + ":14268/api/traces");
    environment.put("OTEL_RESOURCE_ATTRIBUTES", "service.name=smoke-test");
    return environment;
  }
}
