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

package com.splunk.opentelemetry;

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DeclarativeConfigTestExtension implements AfterEachCallback {
  private final AutoCleanupExtension autoCleanupExtension = AutoCleanupExtension.create();

  private DeclarativeConfigTestExtension() {}

  public static DeclarativeConfigTestExtension create() {
    return new DeclarativeConfigTestExtension();
  }

  public OpenTelemetryConfigurationModel getCustomizedModel(String yaml) {
    var configurationModel =
        DeclarativeConfiguration.parse(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    var sdk =
        DeclarativeConfiguration.create(
            configurationModel,
            ComponentLoader.forClassLoader(DeclarativeConfigTestExtension.class.getClassLoader()));
    autoCleanupExtension.deferCleanup(sdk);

    return configurationModel;
  }

  public AutoConfiguredOpenTelemetrySdk createAutoConfiguredSdk(String yaml, Path tempDir)
      throws IOException {
    Path configFilePath = tempDir.resolve("test-config.yaml");
    Files.writeString(configFilePath, yaml);

    var sdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier(
                () -> Map.of("otel.experimental.config.file", configFilePath.toString()))
            .build();
    autoCleanupExtension.deferCleanup(sdk.getOpenTelemetrySdk());

    return sdk;
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    autoCleanupExtension.afterEach(context);
  }
}
