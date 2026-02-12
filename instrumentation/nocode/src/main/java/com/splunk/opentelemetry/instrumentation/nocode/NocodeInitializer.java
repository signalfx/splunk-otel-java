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

package com.splunk.opentelemetry.instrumentation.nocode;

import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfig;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfigProvider;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.isDeclarativeConfig;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@AutoService(BeforeAgentListener.class)
public class NocodeInitializer implements BeforeAgentListener {
  private static final String NOCODE_YMLFILE = "splunk.otel.instrumentation.nocode.yml.file";
  private static final Logger logger = Logger.getLogger(NocodeInitializer.class.getName());

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk sdk) {
    List<NocodeRules.Rule> rules =
        isDeclarativeConfig(sdk)
            ? readDeclarativeConfigRules(getConfigProvider(sdk))
            : readRulesFromFile(getConfig(sdk).getString(NOCODE_YMLFILE));

    NocodeRules.setGlobalRules(rules);
  }

  static List<NocodeRules.Rule> readDeclarativeConfigRules(ConfigProvider configProvider) {
    Objects.requireNonNull(configProvider);
    DeclarativeConfigProperties config = configProvider.getInstrumentationConfig("splunk");

    if (config != null) {
      String rulesFile = config.getString("no_code_file");
      List<DeclarativeConfigProperties> noCodeRules = config.getStructuredList("no_code");

      if (noCodeRules != null) {
        if (rulesFile != null) {
          throw new IllegalStateException(
              "Ambiguous NoCode rules definition (choose declarative config or rules file, not both)");
        }
        return YamlParser.parseFromDeclarativeConfig(noCodeRules);
      }

      return readRulesFromFile(rulesFile);
    }

    return Collections.emptyList();
  }

  static List<NocodeRules.Rule> readRulesFromFile(String yamlFileName) {
    List<NocodeRules.Rule> instrumentationRules = Collections.emptyList();
    if (yamlFileName == null || yamlFileName.trim().isEmpty()) {
      return instrumentationRules;
    }
    try {
      instrumentationRules = YamlParser.parseFromFile(yamlFileName);
      // can throw IllegalArgument and various other RuntimeExceptions too, not just IOException
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Can't load configured nocode yaml.", e);
    }
    return instrumentationRules;
  }
}
