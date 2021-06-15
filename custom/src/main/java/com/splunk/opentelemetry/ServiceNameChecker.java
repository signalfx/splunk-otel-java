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

package com.splunk.opentelemetry;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(AgentListener.class)
public class ServiceNameChecker implements AgentListener {
  private static final Logger log = LoggerFactory.getLogger(ServiceNameChecker.class);

  @Override
  public void beforeAgent(Config config) {
    Map<String, String> resourceAttributes = config.getMapProperty("otel.resource.attributes");
    if (!resourceAttributes.containsKey(ResourceAttributes.SERVICE_NAME.getKey())) {
      log.warn(
          "Resource attribute 'service.name' is not set: your service is unnamed and will be difficult to identify."
              + " Please Set your service name using the 'OTEL_RESOURCE_ATTRIBUTES' environment variable"
              + " or the 'otel.resource.attributes' system property."
              + " E.g. 'export OTEL_RESOURCE_ATTRIBUTES=\"service.name=<YOUR_SERVICE_NAME_HERE>\"'");
    }
  }
}
