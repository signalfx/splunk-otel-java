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

package com.splunk.opentelemetry.jmx;

import static java.util.Collections.emptyList;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizerProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Reserves distribution-owned JMX collection before upstream snapshots instrumentation enablement.
 *
 * <p>The JMX settings stay in place so other model customizers can modify them before {@link
 * JmxMetricsConfigurationCustomizerProvider} merges them. Both providers are temporary until
 * upstream supports inline JMX rules.
 */
@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class JmxUpstreamDisablingCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {
  private static final Logger logger =
      Logger.getLogger(JmxUpstreamDisablingCustomizerProvider.class.getName());

  // Null means upstream retains ownership. Otherwise preserve the user's enablement decision
  // before injecting disabled: [jmx], which is only intended to stop the upstream collector.
  @Nullable private static volatile Boolean originalJmxEnabled;

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(model -> {
      originalJmxEnabled = null;
      if (model.getDistribution() == null) {
        return model;
      }
      DeclarativeConfigProperties distribution =
          DeclarativeConfiguration.toConfigProperties(model.getDistribution());
      if (!distribution.get("splunk").get("java").getPropertyKeys().contains("jmx")) {
        return model;
      }

      DeclarativeConfigProperties instrumentation =
          distribution.get("javaagent").get("instrumentation");
      boolean enabled = isJmxEnabled(instrumentation);

      // Explicit disablement prevents upstream from starting even with default rules.
      // Both JMX nodes stay intact for other customizers and the late merge.
      Map<String, Object> agentInstrumentation = DeclarativeConfigProperties.toMap(instrumentation);
      List<String> disabled =
          new ArrayList<>(instrumentation.getScalarList("disabled", String.class, emptyList()));
      if (!disabled.contains("jmx")) {
        disabled.add("jmx");
      }
      agentInstrumentation.put("disabled", disabled);
      Map<String, Object> javaagent =
          DeclarativeConfigProperties.toMap(distribution.get("javaagent"));
      javaagent.put("instrumentation", agentInstrumentation);
      model.getDistribution().setExtensionProperty("javaagent", javaagent);
      originalJmxEnabled = enabled;
      logger.info("Using distribution.splunk.java.jmx; upstream JMX instrumentation is disabled");
      return model;
    });
  }

  @Override
  public int order() {
    // JavaagentDistributionAccessCustomizerProvider (order 0) copies the enablement settings into
    // AgentDistributionConfig. Changing the model after that cannot disable upstream JMX.
    return -1;
  }

  @Nullable
  static Boolean getOriginalJmxEnabled() {
    return originalJmxEnabled;
  }

  private static boolean isJmxEnabled(DeclarativeConfigProperties instrumentation) {
    if (instrumentation.getScalarList("disabled", String.class, emptyList()).contains("jmx")) {
      return false;
    }
    return instrumentation.getScalarList("enabled", String.class, emptyList()).contains("jmx")
        || instrumentation.getBoolean("default_enabled", true);
  }
}
