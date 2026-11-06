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

package com.splunk.opentelemetry.jmx;

import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.parse;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getDistributionConfig;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getInstrumentationConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.javaagent.extension.instrumentation.internal.AgentDistributionConfig;
import io.opentelemetry.javaagent.tooling.config.JavaagentDistributionAccessCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationBuilder;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class JmxMetricsConfigurationCustomizerProviderTest {
  @Test
  void mergesSettingsAndPreservesInstrumentationAndOtherConfiguration() {
    Logger logger = Logger.getLogger(JmxUpstreamDisablingCustomizerProvider.class.getName());
    Handler handler = mock(Handler.class);
    logger.addHandler(handler);
    OpenTelemetryConfigurationModel model;
    try {
      model =
          customize(
              """
              file_format: "1.2"
              instrumentation/development:
                java:
                  jmx:
                    discovery:
                      delay: 1000
                    config: [legacy.yaml]
                    metrics:
                      included: [legacy.*]
                      excluded: [legacy.ignored]
                  jdbc:
                    enabled: true
              distribution:
                javaagent:
                  exclude_classes: [keep.*]
                  instrumentation:
                    enabled: [jmx, jdbc]
                    disabled: [servlet]
                splunk:
                  other: keep
                  java:
                    other:
                      enabled: true
                    jmx:
                      discovery:
                        delay: 2000
                      metrics:
                        included: [custom.*]
                      rules:
                        - bean: test:type=Monitor
              """);
    } finally {
      logger.removeHandler(handler);
    }

    assertThat(getInstrumentationConfig(model).getPropertyKeys())
        .containsExactlyInAnyOrder("jmx", "jdbc");
    assertThat(DeclarativeConfigProperties.toMap(getInstrumentationConfig(model).get("jmx")))
        .isEqualTo(
            Map.of(
                "discovery", Map.of("delay", 1000L),
                "config", List.of("legacy.yaml"),
                "metrics",
                    Map.of(
                        "included", List.of("legacy.*"), "excluded", List.of("legacy.ignored"))));
    assertThat(getInstrumentationConfig(model).get("jdbc").getBoolean("enabled")).isTrue();
    var jmx = getDistributionConfig(model).get("java").get("jmx");
    assertThat(DeclarativeConfigProperties.toMap(jmx))
        .isEqualTo(
            Map.of(
                "discovery", Map.of("delay", 2000L),
                "config", List.of("legacy.yaml"),
                "metrics",
                    Map.of("included", List.of("custom.*"), "excluded", List.of("legacy.ignored")),
                "rules", List.of(Map.of("bean", "test:type=Monitor"))));
    assertThat(
            DeclarativeConfigProperties.toMap(
                JmxMetricsConfigurationCustomizerProvider.getJmxConfig()))
        .isEqualTo(DeclarativeConfigProperties.toMap(jmx));
    assertThat(getDistributionConfig(model).getString("other")).isEqualTo("keep");
    assertThat(getDistributionConfig(model).get("java").get("other").getBoolean("enabled"))
        .isTrue();
    var javaagent =
        DeclarativeConfiguration.toConfigProperties(model.getDistribution()).get("javaagent");
    assertThat(javaagent.getScalarList("exclude_classes", String.class)).containsExactly("keep.*");
    assertThat(javaagent.get("instrumentation").getScalarList("enabled", String.class))
        .containsExactly("jmx", "jdbc");
    assertThat(javaagent.get("instrumentation").getScalarList("disabled", String.class))
        .containsExactly("servlet", "jmx");

    ArgumentCaptor<LogRecord> records = ArgumentCaptor.forClass(LogRecord.class);
    verify(handler, atLeastOnce()).publish(records.capture());
    assertThat(records.getAllValues())
        .anySatisfy(record -> {
          assertThat(record.getLevel()).isEqualTo(Level.INFO);
          assertThat(record.getMessage()).contains("upstream JMX instrumentation is disabled");
        });
  }

  @Test
  void disablesUpstreamWithoutMergingJmxSettings() {
    var builder = new DeclarativeConfigurationBuilder();
    new JmxUpstreamDisablingCustomizerProvider().customize(builder);
    var model =
        builder.customizeModel(
            parse(
                """
                file_format: '1.2'
                instrumentation/development:
                  java:
                    jmx:
                      discovery:
                        delay: 1000
                distribution:
                  splunk:
                    java:
                      jmx: {}
                """));

    assertThat(getInstrumentationConfig(model).get("jmx").get("discovery").getLong("delay"))
        .isEqualTo(1000);
    assertThat(getDistributionConfig(model).get("java").get("jmx").getPropertyKeys()).isEmpty();
    assertThat(JmxUpstreamDisablingCustomizerProvider.getOriginalJmxEnabled()).isTrue();
    assertThat(
            DeclarativeConfiguration.toConfigProperties(model.getDistribution())
                .get("javaagent")
                .get("instrumentation")
                .getScalarList("disabled", String.class))
        .containsExactly("jmx");
  }

  @Test
  void mergesAfterOtherCustomizersAndBeforeFinalModelCapture() {
    String yaml =
        """
        file_format: '1.2'
        instrumentation/development:
          java:
            jmx:
              discovery:
                delay: 1000
              config: [original.yaml]
        distribution:
          splunk:
            java:
              jmx:
                discovery:
                  delay: 2000
                rules:
                  - bean: test:type=Original
        """;
    var replacement =
        modelCustomizer(
            Integer.MAX_VALUE - 2,
            model -> {
              // Upstream already saw the disablement, but both JMX nodes are still available.
              assertThat(AgentDistributionConfig.get().isInstrumentationEnabled("jmx")).isFalse();
              assertThat(
                      getInstrumentationConfig(model).get("jmx").get("discovery").getLong("delay"))
                  .isEqualTo(1000);
              assertThat(
                      getDistributionConfig(model)
                          .get("java")
                          .get("jmx")
                          .get("discovery")
                          .getLong("delay"))
                  .isEqualTo(2000);
              var updated =
                  parse(
                      yaml.replace("original.yaml", "customized.yaml")
                          .replace("2000", "4000")
                          .replace("test:type=Original", "test:type=Customized"));
              updated
                  .getDistribution()
                  .setExtensionProperty(
                      "javaagent",
                      model.getDistribution().getExtensionProperties().get("javaagent"));
              return updated;
            });
    var capturedConfig = new AtomicReference<Map<String, Object>>();
    var capture =
        modelCustomizer(
            Integer.MAX_VALUE,
            model -> {
              assertThat(
                      DeclarativeConfigProperties.toMap(getInstrumentationConfig(model).get("jmx")))
                  .isEqualTo(
                      Map.of(
                          "discovery", Map.of("delay", 1000L),
                          "config", List.of("customized.yaml")));
              capturedConfig.set(
                  DeclarativeConfigProperties.toMap(
                      JmxMetricsConfigurationCustomizerProvider.getJmxConfig()));
              return model;
            });

    AgentDistributionConfig.resetForTest();
    try {
      var model =
          customize(
              yaml, replacement, capture, new JavaagentDistributionAccessCustomizerProvider());

      assertThat(AgentDistributionConfig.get().isInstrumentationEnabled("jmx")).isFalse();
      var expected =
          Map.of(
              "discovery", Map.of("delay", 4000L),
              "config", List.of("customized.yaml"),
              "rules", List.of(Map.of("bean", "test:type=Customized")));
      assertThat(
              DeclarativeConfigProperties.toMap(
                  getDistributionConfig(model).get("java").get("jmx")))
          .isEqualTo(expected);
      assertThat(capturedConfig.get()).isEqualTo(expected);
    } finally {
      AgentDistributionConfig.resetForTest();
    }
  }

  @Test
  void doesNotStartCustomCollectionWhenDistributionJmxIsAddedTooLate() {
    // Clear any previous startup snapshot before creating the node in a later customizer.
    customize("file_format: '1.2'\ndistribution:\n  splunk:\n    java:\n      jmx: {}\n");
    var addDistributionJmx =
        modelCustomizer(
            1,
            model ->
                parse(
                    """
                    file_format: '1.2'
                    instrumentation/development:
                      java:
                        jmx:
                          config: [upstream.yaml]
                    distribution:
                      splunk:
                        java:
                          jmx: {}
                    """));

    AgentDistributionConfig.resetForTest();
    try {
      var model =
          customize(
              "file_format: '1.2'\n",
              addDistributionJmx,
              new JavaagentDistributionAccessCustomizerProvider());

      assertThat(AgentDistributionConfig.get().isInstrumentationEnabled("jmx")).isTrue();
      assertThat(getInstrumentationConfig(model).get("jmx").getScalarList("config", String.class))
          .containsExactly("upstream.yaml");
      assertThat(JmxMetricsConfigurationCustomizerProvider.getJmxConfig()).isNull();
    } finally {
      AgentDistributionConfig.resetForTest();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "distribution:\n  splunk:\n    java:\n      other: {}\n"})
  void leavesUpstreamUntouchedWithoutDistributionJmx(String distribution) {
    // First populate the startup snapshot to ensure an absent node clears stale state.
    customize("file_format: '1.2'\ndistribution:\n  splunk:\n    java:\n      jmx: {}\n");
    var model =
        customize(
            """
            file_format: "1.2"
            instrumentation/development:
              java:
                jmx:
                  discovery:
                    delay: 3000
            """
                + distribution);

    assertThat(getInstrumentationConfig(model).get("jmx").get("discovery").getLong("delay"))
        .isEqualTo(3000);
    assertThat(JmxMetricsConfigurationCustomizerProvider.getJmxConfig()).isNull();
    if (model.getDistribution() != null) {
      assertThat(model.getDistribution().getExtensionProperties()).doesNotContainKey("javaagent");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"jmx:", "jmx: {}", "jmx:\n  rules: []"})
  void takesOverEvenForAnEmptyDistributionJmxNode(String node) {
    var model =
        customize(
            """
            file_format: '1.2'
            instrumentation/development:
              java:
                jmx:
                  config: [legacy.yaml]
                  discovery:
                    delay: 3000
                  metrics:
                    excluded: [legacy.ignored]
            distribution:
              splunk:
                java:
            """
                + node.indent(6));

    assertThat(JmxMetricsConfigurationCustomizerProvider.getJmxConfig()).isNotNull();
    assertThat(
            JmxMetricsConfigurationCustomizerProvider.getJmxConfig()
                .get("discovery")
                .getLong("delay"))
        .isEqualTo(3000);
    assertThat(
            JmxMetricsConfigurationCustomizerProvider.getJmxConfig()
                .getScalarList("config", String.class))
        .containsExactly("legacy.yaml");
    assertThat(
            DeclarativeConfigProperties.toMap(
                JmxMetricsConfigurationCustomizerProvider.getJmxConfig()))
        .isEqualTo(
            DeclarativeConfigProperties.toMap(getDistributionConfig(model).get("java").get("jmx")));
    assertThat(DeclarativeConfigProperties.toMap(getInstrumentationConfig(model).get("jmx")))
        .isEqualTo(
            Map.of(
                "discovery", Map.of("delay", 3000L),
                "config", List.of("legacy.yaml"),
                "metrics", Map.of("excluded", List.of("legacy.ignored"))));
    assertThat(
            DeclarativeConfiguration.toConfigProperties(model.getDistribution())
                .get("javaagent")
                .get("instrumentation")
                .getScalarList("disabled", String.class))
        .containsExactly("jmx");
  }

  @ParameterizedTest
  @MethodSource("enablement")
  void preservesOriginalEnablementForCustomCollection(String selectors, boolean enabled) {
    var model =
        customize(
            "file_format: '1.2'\ndistribution:\n  javaagent:\n    instrumentation:\n"
                + selectors.indent(6)
                + "  splunk:\n    java:\n      jmx: {}\n");

    assertThat(JmxMetricsConfigurationCustomizerProvider.getJmxConfig() != null).isEqualTo(enabled);
    assertThat(
            DeclarativeConfiguration.toConfigProperties(model.getDistribution())
                .get("javaagent")
                .get("instrumentation")
                .getScalarList("disabled", String.class))
        .containsOnlyOnce("jmx");
  }

  private static Stream<Arguments> enablement() {
    return Stream.of(
        Arguments.of("default_enabled: false", false),
        Arguments.of("default_enabled: false\nenabled: [jmx]", true),
        Arguments.of("enabled: [jmx]\ndisabled: [jmx]", false),
        Arguments.of("disabled: [jmx]", false),
        Arguments.of("disabled: [jdbc]", true));
  }

  private static OpenTelemetryConfigurationModel customize(
      String yaml, DeclarativeConfigurationCustomizerProvider... additionalProviders) {
    var builder = new DeclarativeConfigurationBuilder();
    Stream.concat(
            Stream.of(
                new JmxMetricsConfigurationCustomizerProvider(),
                new JmxUpstreamDisablingCustomizerProvider()),
            Arrays.stream(additionalProviders))
        .sorted(Comparator.comparingInt(DeclarativeConfigurationCustomizerProvider::order))
        .forEach(provider -> provider.customize(builder));
    return builder.customizeModel(parse(yaml));
  }

  private static DeclarativeConfigurationCustomizerProvider modelCustomizer(
      int order, UnaryOperator<OpenTelemetryConfigurationModel> customization) {
    return new DeclarativeConfigurationCustomizerProvider() {
      @Override
      public void customize(DeclarativeConfigurationCustomizer customizer) {
        customizer.addModelCustomizer(customization);
      }

      @Override
      public int order() {
        return order;
      }
    };
  }
}
