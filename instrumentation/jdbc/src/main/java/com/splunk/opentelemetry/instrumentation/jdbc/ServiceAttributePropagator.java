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

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME;
import static io.opentelemetry.semconv.incubating.ServiceIncubatingAttributes.SERVICE_NAMESPACE;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Collection;
import java.util.Collections;

class ServiceAttributePropagator implements TextMapPropagator {
  private final String serviceName;
  private final String serviceNamespace;
  private final String deploymentEnvironment;

  ServiceAttributePropagator(
      String serviceName, String serviceNamespace, String deploymentEnvironment) {
    this.serviceName = serviceName;
    this.serviceNamespace = serviceNamespace;
    this.deploymentEnvironment = deploymentEnvironment;
  }

  @Override
  public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
    setIfNotNull(setter, carrier, SERVICE_NAME.getKey(), serviceName);
    setIfNotNull(setter, carrier, SERVICE_NAMESPACE.getKey(), serviceNamespace);
    setIfNotNull(setter, carrier, DEPLOYMENT_ENVIRONMENT_NAME.getKey(), deploymentEnvironment);
  }

  private static <C> void setIfNotNull(
      TextMapSetter<C> setter, C carrier, String key, String value) {
    if (value != null) {
      setter.set(carrier, key, value);
    }
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
