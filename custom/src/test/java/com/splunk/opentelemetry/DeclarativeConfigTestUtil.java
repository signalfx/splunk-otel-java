package com.splunk.opentelemetry;

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DeclarativeConfigTestUtil {
  public static OpenTelemetryConfigurationModel getCustomizedModel(String yaml) {
    OpenTelemetryConfigurationModel configurationModel =
        DeclarativeConfiguration.parse(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    DeclarativeConfiguration.create(
        configurationModel,
        ComponentLoader.forClassLoader(
            SplunkDeclarativeConfigurationTest.class.getClassLoader()));

    return configurationModel;
  }

  public static AutoConfiguredOpenTelemetrySdk createAutoConfiguredSdk(String yaml, Path tempDir)
      throws IOException {
    Path configFilePath = tempDir.resolve("test-config.yaml");
    Files.writeString(configFilePath, yaml);
    System.setProperty("otel.experimental.config.file", configFilePath.toString());

    return AutoConfiguredOpenTelemetrySdk.builder().build();
  }
}
