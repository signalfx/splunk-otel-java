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

import static com.splunk.opentelemetry.appd.AppdBonusPropagator.CTX_HEADER_ENV;
import static com.splunk.opentelemetry.appd.AppdBonusPropagator.CTX_HEADER_SERVICE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class AppdBeforeAgentListenerTest {
  @RegisterExtension final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

  @AfterEach
  void resetDeclarativeConfigSuppliers() {
    ProfilerDeclarativeConfiguration.SUPPLIER.reset();
    SnapshotProfilingDeclarativeConfiguration.SUPPLIER.reset();
  }

  @Test
  void shouldSetPropagatorProperties(@TempDir Path tempDir) throws IOException {
    // given
    AppdBeforeAgentListener agentListener = new AppdBeforeAgentListener();
    var yaml =
        """
            file_format: "1.0-rc.3"
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
        DeclarativeConfigTestUtil.createAutoConfiguredSdk(yaml, tempDir, autoCleanup);

    // when
    agentListener.beforeAgent(autoConfiguredOpenTelemetrySdk);

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

    assertThat(carrier.get(CTX_HEADER_SERVICE)).isEqualTo("test-service");
    assertThat(carrier.get(CTX_HEADER_ENV)).isEqualTo("test-deployment-env");
  }
}
