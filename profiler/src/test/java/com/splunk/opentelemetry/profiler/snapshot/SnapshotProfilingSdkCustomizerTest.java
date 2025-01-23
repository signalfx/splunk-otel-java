package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingSdkCustomizer.ActivationNotifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SnapshotProfilingSdkCustomizerTest {
  private final ObservableActivationNotifier activationNotifier = new ObservableActivationNotifier();
  private final SnapshotProfilingSdkCustomizer customizer = new SnapshotProfilingSdkCustomizer(activationNotifier);

  @Nested
  class TestSnapshotProfilingDisabledByDefault {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s = OpenTelemetrySdkExtension.builder().with(customizer).build();

    @Test
    void customizeOpenTelemetrySdk() {
      assertFalse(activationNotifier.activated);
    }
  }

  @Nested
  class TestEnableSnapshotProfiling {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s = OpenTelemetrySdkExtension.builder()
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
    public final OpenTelemetrySdkExtension s = OpenTelemetrySdkExtension.builder()
        .with(customizer)
        .withProperty("splunk.snapshot.profiler.enabled", "false")
        .build();

    @Test
    void customizeOpenTelemetrySdk() {
      assertFalse(activationNotifier.activated);
    }
  }

  private static class ObservableActivationNotifier implements ActivationNotifier {
    private boolean activated = false;

    @Override
    public void activated() {
      this.activated = true;
    }
  }
}
