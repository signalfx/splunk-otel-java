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

package com.splunk.opentelemetry.instrumentation.jdbc;

import static com.splunk.opentelemetry.instrumentation.jdbc.SqlCommenterInitializer.defaultPropagator;
import static com.splunk.opentelemetry.instrumentation.jdbc.SqlCommenterInitializer.traceContextPropagator;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getResource;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME;
import static io.opentelemetry.semconv.incubating.ServiceIncubatingAttributes.SERVICE_NAMESPACE;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;

@AutoService(AgentListener.class)
public class PropagatorInitializer implements AgentListener {

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk sdk) {
    Resource resource = getResource(sdk);
    String serviceName = resource.getAttribute(SERVICE_NAME);
    String serviceNamespace = resource.getAttribute(SERVICE_NAMESPACE);
    String deploymentEnvironment = resource.getAttribute(DEPLOYMENT_ENVIRONMENT_NAME);

    defaultPropagator =
        new ServiceAttributePropagator(serviceName, serviceNamespace, deploymentEnvironment);
    traceContextPropagator =
        TextMapPropagator.composite(defaultPropagator, W3CTraceContextPropagator.getInstance());
  }
}
