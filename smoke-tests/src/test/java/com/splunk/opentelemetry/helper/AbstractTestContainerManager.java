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
  protected static final int BACKEND_PORT = 8080;
  protected static final int HEC_BACKEND_PORT = 1080;
  protected static final int COLLECTOR_PORT = 4318;

  protected static final String BACKEND_ALIAS = "backend";
  protected static final String HEC_BACKEND_ALIAS = "hec-backend";
  protected static final String COLLECTOR_ALIAS = "collector";
  protected static final String COLLECTOR_CONFIG_RESOURCE = "/otel.yaml";

  protected Map<String, String> getAgentEnvironment(
      String jvmArgsEnvVarName, boolean setServiceName) {
    Map<String, String> environment = new HashMap<>();
    // while modern JVMs understand linux container memory limits, they do not understand windows
    // container memory limits yet, so we need to explicitly set max heap in order to prevent the
    // JVM from taking too much memory and hitting the windows container memory limit
    environment.put(jvmArgsEnvVarName, "-Xmx512m -javaagent:/" + TARGET_AGENT_FILENAME);
    environment.put("OTEL_BSP_MAX_EXPORT_BATCH_SIZE", "1");
    environment.put("OTEL_BSP_SCHEDULE_DELAY", "10ms");
    environment.put(
        "OTEL_EXPORTER_OTLP_ENDPOINT", "http://" + COLLECTOR_ALIAS + ":" + COLLECTOR_PORT);
    // export metrics every 1s
    environment.put("OTEL_METRIC_EXPORT_INTERVAL", "1000");
    if (setServiceName) {
      environment.put("OTEL_RESOURCE_ATTRIBUTES", "service.name=smoke-test");
    }
    // This does not affect tests in any way but serves to verify that agent can actually load this
    // sampler
    environment.put("OTEL_TRACES_SAMPLER", "internal_root_off");
    environment.put("OTEL_TRACES_EXPORTER", "otlp,console");

    return environment;
  }
}
