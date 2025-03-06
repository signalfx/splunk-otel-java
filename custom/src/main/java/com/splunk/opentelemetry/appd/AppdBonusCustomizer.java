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

package com.splunk.opentelemetry.appd;

import static com.splunk.opentelemetry.appd.AppdBonusConstants.CONFIG_CISCO_CTX_ENABLED;
import static com.splunk.opentelemetry.appd.AppdBonusConstants.PROPAGATOR_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.extension.incubator.trace.OnStartSpanProcessor;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoService(AutoConfigurationCustomizerProvider.class)
public final class AppdBonusCustomizer implements AutoConfigurationCustomizerProvider {

  static final List<String> DEFAULT_PROPAGATORS = Arrays.asList("tracecontext", "baggage");

  @Override
  public void customize(AutoConfigurationCustomizer customizer) {
    customize(customizer, AppdBonusPropagator.getInstance());
  }

  @VisibleForTesting
  void customize(AutoConfigurationCustomizer customizer, AppdBonusPropagator propagator) {
    customizer.addPropertiesCustomizer(this::customizeProperties);

    customizer.addTracerProviderCustomizer(
        (builder, config) -> {
          if (featureEnabled(config)) {
            SpanProcessor processor = OnStartSpanProcessor.create(new AppdBonusSpanProcessor());
            return builder.addSpanProcessor(processor);
          }
          return builder;
        });
    customizer.addResourceCustomizer(
        (resource, configProperties) -> {
          propagator.setEnvironmentName(resource.getAttribute(DEPLOYMENT_ENVIRONMENT_NAME));
          propagator.setServiceName(resource.getAttribute(SERVICE_NAME));
          return resource;
        });
  }

  private static boolean featureEnabled(ConfigProperties config) {
    return config.getBoolean(CONFIG_CISCO_CTX_ENABLED, false);
  }

  /** Used to add the AppD propagator name to the otel.propagators list if configured. */
  private Map<String, String> customizeProperties(ConfigProperties config) {
    if (!config.getBoolean(CONFIG_CISCO_CTX_ENABLED, false)) {
      return Collections.emptyMap();
    }
    List<String> existing = config.getList("otel.propagators", DEFAULT_PROPAGATORS);
    if (existing.contains(PROPAGATOR_NAME)) {
      return Collections.emptyMap();
    }
    List<String> propagators = new ArrayList<>(existing);
    propagators.add(PROPAGATOR_NAME);
    Map<String, String> customized = new HashMap<>();
    customized.put("otel.propagators", String.join(",", propagators));
    return customized;
  }
}
