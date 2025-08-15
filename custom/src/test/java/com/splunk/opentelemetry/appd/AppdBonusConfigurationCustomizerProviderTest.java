package com.splunk.opentelemetry.appd;

import static com.splunk.opentelemetry.DeclarativeConfigTestUtil.parseAndCustomizeModel;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import org.junit.jupiter.api.Test;

class AppdBonusConfigurationCustomizerProviderTest {

  @Test
  void shouldAddPropagatorAndSpanProcessorWhenFeatureIsEnabled () {
    var yaml =
        """
            file_format: "1.0-rc.1"
            instrumentation/development:
              java:
                cisco:
                   ctx:
                     enabled: true
            """;

    AppdBonusConfigurationCustomizerProvider customizer =
        new AppdBonusConfigurationCustomizerProvider();
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    assertThat(model.getPropagator().getCompositeList()).isEqualTo("appd-bonus");
    assertThat(model.getTracerProvider().getProcessors()).hasSize(1);
    assertThat(model.getTracerProvider().getProcessors().getFirst().getAdditionalProperties()).hasSize(1);
    assertThat(model.getTracerProvider().getProcessors().getFirst().getAdditionalProperties()).containsKey("appd-bonus");
  }

  @Test
  void shouldNotAddPropagatorAndSpanProcessorWhenFeatureIsDisabled () {
    var yaml =
        """
            file_format: "1.0-rc.1"
            instrumentation/development:
              java:
                cisco:
                   ctx:
                     enabled: false
            """;

    AppdBonusConfigurationCustomizerProvider customizer =
        new AppdBonusConfigurationCustomizerProvider();
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    assertThat(model.getPropagator()).isNull();
    assertThat(model.getTracerProvider()).isNull();
  }

  @Test
  void shouldNotAddPropagatorAndSpanProcessorWhenFeaturePropertyIsMissing () {
    var yaml =
        """
            file_format: "1.0-rc.1"
            instrumentation/development:
              java:
            """;

    AppdBonusConfigurationCustomizerProvider customizer =
        new AppdBonusConfigurationCustomizerProvider();
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    assertThat(model.getPropagator()).isNull();
    assertThat(model.getTracerProvider()).isNull();
  }
}
