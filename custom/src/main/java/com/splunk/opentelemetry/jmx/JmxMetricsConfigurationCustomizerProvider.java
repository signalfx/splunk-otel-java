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

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalInstrumentationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalLanguageSpecificInstrumentationPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.OpenTelemetryConfigurationModelAccessor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Merges and captures JMX settings after other model customizations.
 *
 * <p>{@link JmxUpstreamDisablingCustomizerProvider} handles upstream disablement earlier because
 * upstream snapshots enablement before the model is fully customized. Keeping the merge here
 * preserves subsequent changes to either JMX node, including replacement of the model itself. The
 * instrumentation node stays intact; upstream disablement prevents duplicate collection. Both
 * providers are temporary until upstream supports inline JMX rules.
 */
@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class JmxMetricsConfigurationCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {
  private static final Logger logger =
      Logger.getLogger(JmxMetricsConfigurationCustomizerProvider.class.getName());

  // Null means there is no enabled distribution-owned collection to start.
  @Nullable private static volatile DeclarativeConfigProperties jmxConfig;

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(model -> {
      jmxConfig = null;
      // Only take ownership if the early provider disabled upstream before its snapshot.
      // In particular, a distribution JMX node added too late must not start a second collector.
      Boolean enabled = JmxUpstreamDisablingCustomizerProvider.getOriginalJmxEnabled();
      if (enabled == null || model.getDistribution() == null) {
        return model;
      }
      DeclarativeConfigProperties distribution =
          DeclarativeConfiguration.toConfigProperties(model.getDistribution());
      DeclarativeConfigProperties splunkJava = distribution.get("splunk").get("java");
      if (!splunkJava.getPropertyKeys().contains("jmx")) {
        return model;
      }

      Map<String, Object> upstreamConfig = Collections.emptyMap();
      ExperimentalInstrumentationModel instrumentationModel =
          OpenTelemetryConfigurationModelAccessor.getInstrumentation(model);
      if (instrumentationModel != null
          && instrumentationModel.getJava() != null
          && instrumentationModel.getJava().getAdditionalProperties().containsKey("jmx")) {
        ExperimentalLanguageSpecificInstrumentationPropertyModel upstream =
            instrumentationModel.getJava().getAdditionalProperties().get("jmx");
        if (upstream != null) {
          upstreamConfig = upstream.getAdditionalProperties();
        }
        logger.fine(
            "Merging instrumentation/development.java.jmx into distribution.splunk.java.jmx");
      }

      Map<String, Object> merged =
          merge(upstreamConfig, DeclarativeConfigProperties.toMap(splunkJava.get("jmx")));
      Map<String, Object> javaConfig = DeclarativeConfigProperties.toMap(splunkJava);
      javaConfig.put("jmx", merged);
      Map<String, Object> splunkConfig =
          DeclarativeConfigProperties.toMap(distribution.get("splunk"));
      splunkConfig.put("java", javaConfig);
      model.getDistribution().setExtensionProperty("splunk", splunkConfig);

      // Use the original decision, not the disabled: [jmx] injected for the upstream collector.
      if (enabled) {
        jmxConfig = DeclarativeConfiguration.toConfigProperties(merged);
      }
      return model;
    });
  }

  @Override
  public int order() {
    // Merge after ordinary customizations. Reserve Integer.MAX_VALUE for final model capture,
    // including the OpAMP DeclarativeConfigurationInterceptor.
    return Integer.MAX_VALUE - 1;
  }

  @Nullable
  static DeclarativeConfigProperties getJmxConfig() {
    return jmxConfig;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> merge(
      Map<String, Object> defaults, Map<String, Object> overrides) {
    Map<String, Object> result = new LinkedHashMap<>(defaults);
    overrides.forEach((key, value) -> {
      Object previous = result.get(key);
      result.put(
          key,
          previous instanceof Map && value instanceof Map
              ? merge((Map<String, Object>) previous, (Map<String, Object>) value)
              : value);
    });
    return result;
  }
}
