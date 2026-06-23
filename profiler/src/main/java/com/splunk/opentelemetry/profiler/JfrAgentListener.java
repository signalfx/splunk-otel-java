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
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
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
    ProfilingSupervisor.setupJfrContextStorage();

    // Always start the supervisor, so it can start profiling later elsewhere.
    ProfilingSupervisor supervisor = makeProfilingSupervisor(sdk);

    ProfilerConfiguration config = ProfilerConfiguration.SUPPLIER.get();
    if (notClearForTakeoff(config)) {
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
