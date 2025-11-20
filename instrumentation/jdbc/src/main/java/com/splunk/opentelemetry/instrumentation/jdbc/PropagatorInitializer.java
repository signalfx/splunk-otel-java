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

import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getResource;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Collection;
import java.util.Collections;

@AutoService(AgentListener.class)
public class PropagatorInitializer implements AgentListener {
  // propagates service.name attribute
  static TextMapPropagator defaultPropagator = TextMapPropagator.noop();
  // propagates service.name attribute and traceparent
  static TextMapPropagator traceContextPropagator = W3CTraceContextPropagator.getInstance();

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk sdk) {
    Resource resource = getResource(sdk);
    String serviceName = resource.getAttribute(SERVICE_NAME);
    if (!"unknown_service:java".equals(serviceName)) {
      defaultPropagator = new ServiceAttributePropagator(serviceName);
      traceContextPropagator =
          TextMapPropagator.composite(defaultPropagator, traceContextPropagator);
    }
  }

  private static class ServiceAttributePropagator implements TextMapPropagator {
    private final String serviceName;

    ServiceAttributePropagator(String serviceName) {
      this.serviceName = serviceName;
    }

    @Override
    public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
      setter.set(carrier, SERVICE_NAME.getKey(), serviceName);
    }

    @Override
    public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
      return context;
    }

    @Override
    public Collection<String> fields() {
      return Collections.emptyList();
    }
  }
}
