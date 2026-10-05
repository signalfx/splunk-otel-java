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
import com.splunk.opentelemetry.profiler.ProfilerConfiguration.CpuMode;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.logging.Logger;

@AutoService({AgentListener.class, BeforeAgentListener.class})
public class ProfilerAgentListener implements AgentListener, BeforeAgentListener {
  private static final Logger logger = Logger.getLogger(ProfilerAgentListener.class.getName());
  private final JFR jfr;

  public ProfilerAgentListener() {
    this(JFR.getInstance());
  }

  @VisibleForTesting
  ProfilerAgentListener(JFR jfr) {
    this.jfr = jfr;
  }

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk sdk) {
    ProfilingSupervisor.setupJfrContextStorage();
  }

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk sdk) {
    // Always start the supervisor, so it can start profiling later elsewhere.
    ProfilingSupervisor supervisor = makeProfilingSupervisor(sdk);

    ProfilerConfiguration config = ProfilerConfiguration.SUPPLIER.get();
    if (!config.isEnabled()) {
      logger.fine("Profiler is not enabled.");
      return;
    }

    CpuMode cpuMode = config.getCpuMode();
    if ((cpuMode == CpuMode.JFR || config.getMemoryEnabled()) && !jfr.isAvailable()) {
      logger.warning(
          "JDK Flight Recorder (JFR) is not available in this JVM. Profiling is disabled.");
      return;
    }

    supervisor.requestStartProfiling();
  }

  @Override
  public int order() {
    // Run it a bit earlier than listeners with default priority
    return -1;
  }

  // Exists for testing
  ProfilingSupervisor makeProfilingSupervisor(AutoConfiguredOpenTelemetrySdk sdk) {
    return ProfilingSupervisor.createAndStart(sdk);
  }
}
