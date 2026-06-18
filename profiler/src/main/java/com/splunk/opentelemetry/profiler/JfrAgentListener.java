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
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.logging.Logger;

@AutoService(AgentListener.class)
public class JfrAgentListener implements AgentListener {
  private static final Logger logger = Logger.getLogger(JfrAgentListener.class.getName());
  private final JFR jfr;

  public JfrAgentListener() {
    this(JFR.getInstance());
  }

  @VisibleForTesting
  JfrAgentListener(JFR jfr) {
    this.jfr = jfr;
  }

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk sdk) {

    ProfilerConfiguration config = getProfilerConfiguration(sdk);
    // Always start the supervisor, so it can start profiling later elsewhere.
    ProfilingSupervisor supervisor = makeProfilingSupervisor(sdk, config);

    if (notClearForTakeoff(config)) {
      return;
    }
    supervisor.requestStartProfiling();
  }

  // Exists for testing
  ProfilingSupervisor makeProfilingSupervisor(
      AutoConfiguredOpenTelemetrySdk sdk, ProfilerConfiguration config) {
    return ProfilingSupervisor.createAndStart(sdk, config);
  }

  private static ProfilerConfiguration getProfilerConfiguration(
      AutoConfiguredOpenTelemetrySdk sdk) {
    if (ProfilerDeclarativeConfiguration.SUPPLIER.isConfigured()) {
      return ProfilerDeclarativeConfiguration.SUPPLIER.get();
    } else {
      ConfigProperties configProperties = AutoConfigureUtil.getConfig(sdk);
      return new ProfilerEnvVarsConfiguration(configProperties);
    }
  }

  private boolean notClearForTakeoff(ProfilerConfiguration config) {
    if (!config.isEnabled()) {
      logger.fine("Profiler is not enabled.");
      return true;
    }
    if (!jfr.isAvailable()) {
      logger.warning(
          "JDK Flight Recorder (JFR) is not available in this JVM. Profiling is disabled.");
      return true;
    }

    return false;
  }
}
