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
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.extension.incubator.trace.OnStartSpanProcessor;
import io.opentelemetry.sdk.trace.SpanProcessor;

@AutoService(AutoConfigurationCustomizerProvider.class)
public final class AppdBonusCustomizer implements AutoConfigurationCustomizerProvider {

  @Override
  public void customize(AutoConfigurationCustomizer customizer) {
    customize(customizer, new AppdBonusPropagator());
  }

  @VisibleForTesting
  void customize(AutoConfigurationCustomizer customizer, AppdBonusPropagator propagator) {
    customizer.addSpanProcessorCustomizer(
        (spanProcessor, config) -> {
          if (featureEnabled(config)) {
            SpanProcessor processor = OnStartSpanProcessor.create(new AppdBonusSpanProcessor());
            return SpanProcessor.composite(spanProcessor, processor);
          }
          return spanProcessor;
        });
    customizer.addResourceCustomizer(
        (resource, configProperties) -> {
          propagator.setEnvironmentName(resource.getAttribute(DEPLOYMENT_ENVIRONMENT_NAME));
          propagator.setServiceName(resource.getAttribute(SERVICE_NAME));
          return resource;
        });
    customizer.addPropagatorCustomizer(
        (existing, config) -> {
          if (featureEnabled(config)) {
            return TextMapPropagator.composite(existing, propagator);
          }
          return existing;
        });
  }

  private static boolean featureEnabled(ConfigProperties config) {
    return config.getBoolean(CONFIG_CISCO_CTX_ENABLED, false);
  }
}
