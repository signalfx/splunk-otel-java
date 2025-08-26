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

import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.javaagent.extension.internal.DeclarativeConfigPropertiesBridgeBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.SdkAutoconfigureAccess;
import io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationBuilder;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DeclarativeConfigTestUtil {
  private DeclarativeConfigTestUtil() {}

  public static OpenTelemetryConfigurationModel parseModel(String yaml) {
    try (InputStream yamlStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
      return DeclarativeConfiguration.parse(yamlStream);
    } catch (IOException e) {
      throw new RuntimeException("Could not parse YAML from string", e);
    }
  }

  public static OpenTelemetryConfigurationModel parseAndCustomizeModel(
      String yaml, DeclarativeConfigurationCustomizerProvider customizer) {
    OpenTelemetryConfigurationModel model = parseModel(yaml);
    DeclarativeConfigurationBuilder builder = new DeclarativeConfigurationBuilder();
    customizer.customize(builder);

    builder.customizeModel(model);
    return model;
  }

  public static AutoConfiguredOpenTelemetrySdk createAutoConfiguredSdk(String yaml, Path tempDir)
      throws IOException {
    Path configFilePath = tempDir.resolve("test-config.yaml");
    Files.writeString(configFilePath, yaml);

    var autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier(
                () -> Map.of("otel.experimental.config.file", configFilePath.toString()))
            .build();

    ConfigProvider configProvider = AutoConfigureUtil.getConfigProvider(autoConfiguredSdk);
    OpenTelemetrySdk sdk = autoConfiguredSdk.getOpenTelemetrySdk();

    if (configProvider != null) {
      return SdkAutoconfigureAccess.create(
          sdk,
          SdkAutoconfigureAccess.getResource(autoConfiguredSdk),
          new DeclarativeConfigPropertiesBridgeBuilder()
              .buildFromInstrumentationConfig(configProvider.getInstrumentationConfig()),
          configProvider);
    }

    return autoConfiguredSdk;
  }
}
