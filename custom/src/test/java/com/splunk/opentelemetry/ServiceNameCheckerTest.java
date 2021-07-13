package com.splunk.opentelemetry;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.opentelemetry.instrumentation.api.config.Config;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceNameCheckerTest {
  @Mock
  Consumer<String> logWarn;

  @Test
  void shouldLogWarnWhenNeitherServiceNameNorResourceAttributeIsConfigured() {
    // given
    var config = Config.newBuilder().build();

    var underTest = new ServiceNameChecker(logWarn);

    // when
    underTest.beforeAgent(config);

    // then
    verify(logWarn).accept(anyString());
  }

  @Test
  void shouldNotLogWarnWhenServiceNameIsConfigured() {
    // given
    var config = Config.newBuilder()
        .readProperties(Map.of("otel.service.name", "test"))
        .build();

    var underTest = new ServiceNameChecker(logWarn);

    // when
    underTest.beforeAgent(config);

    // then
    verifyNoInteractions(logWarn);
  }

  @Test
  void shouldNotLogWarnWhenResourceAttributeIsConfigured() {
    // given
    var config = Config.newBuilder()
        .readProperties(Map.of("otel.resource.attributes", "service.name=test"))
        .build();

    var underTest = new ServiceNameChecker(logWarn);

    // when
    underTest.beforeAgent(config);

    // then
    verifyNoInteractions(logWarn);
  }
}