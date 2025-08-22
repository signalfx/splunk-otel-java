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

import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfig;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getResource;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.util.logging.Logger;

@AutoService(BeforeAgentListener.class)
public class AppdInstrumentationListener implements BeforeAgentListener {
  private static final Logger logger =
      Logger.getLogger(AppdInstrumentationListener.class.getName());

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties config = getConfig(autoConfiguredOpenTelemetrySdk);
    if (!config.getBoolean("cisco.ctx.enabled", false)) {
      return;
    }

    Resource resource = getResource(autoConfiguredOpenTelemetrySdk);
    logger.fine(() -> "Setting up AppdBonusPropagator with resource: " + resource);

    AppdBonusPropagator appdBonusPropagator = AppdBonusPropagator.getInstance();
    appdBonusPropagator.setEnvironmentName(resource.getAttribute(DEPLOYMENT_ENVIRONMENT_NAME));
    appdBonusPropagator.setServiceName(resource.getAttribute(SERVICE_NAME));
  }
}
