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

package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SnapshotProfilingSdkCustomizerTest {
  private final ObservableActivationNotifier activationNotifier =
      new ObservableActivationNotifier();
  private final SnapshotProfilingSdkCustomizer customizer =
      new SnapshotProfilingSdkCustomizer(activationNotifier);

  @Nested
  class TestSnapshotProfilingDisabledByDefault {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.builder().with(customizer).build();

    @Test
    void customizeOpenTelemetrySdk() {
      assertFalse(activationNotifier.activated);
    }
  }

  @Nested
  class TestEnableSnapshotProfiling {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.builder()
            .with(customizer)
            .withProperty("splunk.snapshot.profiler.enabled", "true")
            .build();

    @Test
    void customizeOpenTelemetrySdk() {
      assertTrue(activationNotifier.activated);
    }
  }

  @Nested
  class TestDisableSnapshotProfiling {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.builder()
            .with(customizer)
            .withProperty("splunk.snapshot.profiler.enabled", "false")
            .build();

    @Test
    void customizeOpenTelemetrySdk() {
      assertFalse(activationNotifier.activated);
    }
  }

  private static class ObservableActivationNotifier implements Runnable {
    private boolean activated = false;

    @Override
    public void run() {
      this.activated = true;
    }
  }
}
