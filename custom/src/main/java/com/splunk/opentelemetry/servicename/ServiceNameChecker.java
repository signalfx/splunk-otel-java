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

package com.splunk.opentelemetry.servicename;

import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfig;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getResource;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ServiceAttributes;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

@AutoService(BeforeAgentListener.class)
public class ServiceNameChecker implements BeforeAgentListener {
  private static final Logger logger = Logger.getLogger(ServiceNameChecker.class.getName());

  private final Consumer<String> logWarn;

  @SuppressWarnings("unused")
  public ServiceNameChecker() {
    this(logger::warning);
  }

  // visible for tests
  ServiceNameChecker(Consumer<String> logWarn) {
    this.logWarn = logWarn;
  }

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties config = getConfig(autoConfiguredOpenTelemetrySdk);
    Resource resource = getResource(autoConfiguredOpenTelemetrySdk);
    if (serviceNameNotConfigured(config, resource)) {
      if (AutoConfigureUtil.isDeclarativeConfig(autoConfiguredOpenTelemetrySdk)) {
        logWarn.accept(
            "The service.name resource attribute is not set. Your service is unnamed and will be difficult to identify.\n"
                + " Set your service name in '.resource.attributes' node, or specify appropriate resource detector in the configuration YAML file.");
      } else {
        logWarn.accept(
            "The service.name resource attribute is not set. Your service is unnamed and will be difficult to identify.\n"
                + " Set your service name using the OTEL_SERVICE_NAME or OTEL_RESOURCE_ATTRIBUTES environment variable.\n"
                + " E.g. `OTEL_SERVICE_NAME=\"<YOUR_SERVICE_NAME_HERE>\"`");
      }
    }
  }

  // make sure this listener is one of the first things run by the agent
  @Override
  public int order() {
    return -100;
  }

  static boolean serviceNameNotConfigured(ConfigProperties config, Resource resource) {
    String serviceName = config.getString("otel.service.name");
    Map<String, String> resourceAttributes = config.getMap("otel.resource.attributes");
    return serviceName == null
        && !resourceAttributes.containsKey(ServiceAttributes.SERVICE_NAME.getKey())
        && "unknown_service:java".equals(resource.getAttribute(ServiceAttributes.SERVICE_NAME));
  }
}
