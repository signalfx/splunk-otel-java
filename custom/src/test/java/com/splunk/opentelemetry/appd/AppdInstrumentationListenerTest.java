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

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class AppdInstrumentationListenerTest {
  @RegisterExtension final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

  @Test
  void shouldSetPropagatorProperties(@TempDir Path tempDir) throws IOException {
    // given
    AppdInstrumentationListener listener = new AppdInstrumentationListener();
    var yaml =
        """
            file_format: "1.0-rc.1"
            resource:
              attributes:
                - name: service.name
                  value: test-service
                - name: deployment.environment.name
                  value: test-deployment-env
            instrumentation/development:
              java:
                cisco:
                   ctx:
                     enabled: true
            """;
    AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk =
        createAutoConfiguredSdk(yaml, tempDir);

    // when
    listener.beforeAgent(autoConfiguredOpenTelemetrySdk);

    // then
    AppdBonusPropagator propagator = AppdBonusPropagator.getInstance();
    Map<String, String> carrier = new HashMap<>();
    Context context = Context.current();
    propagator.inject(
        context,
        carrier,
        (map, key, value) -> {
          if (map != null) {
            map.put(key, value);
          }
        });

    assertThat(carrier.get(SERVICE_NAME.getKey())).isEqualTo("test-service");
    assertThat(carrier.get(DEPLOYMENT_ENVIRONMENT_NAME.getKey())).isEqualTo("test-deployment-env");
  }

  private AutoConfiguredOpenTelemetrySdk createAutoConfiguredSdk(String yaml, Path tempDir)
      throws IOException {
    Path configFilePath = tempDir.resolve("test-config.yaml");
    Files.writeString(configFilePath, yaml);

    var sdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier(
                () -> Map.of("otel.experimental.config.file", configFilePath.toString()))
            .build();
    autoCleanup.deferCleanup(sdk.getOpenTelemetrySdk());

    return sdk;
  }
}
