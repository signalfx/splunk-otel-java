package com.splunk.opentelemetry.profiler.snapshot;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.util.logging.Logger;

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER;
import static java.util.Collections.emptyMap;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class SnapshotProfilingSdkCustomizer implements AutoConfigurationCustomizerProvider {
  private static final Logger LOGGER = Logger.getLogger(SnapshotProfilingSdkCustomizer.class.getName());

  private final ActivationNotifier activationNotifier;

  public SnapshotProfilingSdkCustomizer() {
    this(() -> LOGGER.info("Snapshot profiling activated"));
  }

  @VisibleForTesting
  SnapshotProfilingSdkCustomizer(ActivationNotifier activationNotifier) {
    this.activationNotifier = activationNotifier;
  }

  @Override
  public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
    autoConfigurationCustomizer.addPropertiesCustomizer(
        config -> {
          if (snapshotProfilingEnabled(config)) {
            activationNotifier.activated();
          }
          return emptyMap();
        });
  }

  private boolean snapshotProfilingEnabled(ConfigProperties config) {
    return config.getBoolean(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, false);
  }

  interface ActivationNotifier {
    void activated();
  }
}
