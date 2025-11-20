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
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ServiceAttributePropagatorTest {

  @Test
  void testPropagation() {
    TextMapPropagator propagator =
        new ServiceAttributePropagator(
            "service-name", "service-namespace", "deployment-environment");
    Map<String, String> carrier = new LinkedHashMap<>();
    propagator.inject(Context.root(), carrier, (map, key, value) -> map.put(key, value));

    assertThat(carrier)
        .containsExactly(
            entry(SERVICE_NAME.getKey(), "service-name"),
            entry(SERVICE_NAMESPACE.getKey(), "service-namespace"),
            entry(DEPLOYMENT_ENVIRONMENT_NAME.getKey(), "deployment-environment"));
  }
}
