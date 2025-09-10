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

package com.splunk.opentelemetry.resource;

import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_FULL_COMMAND_LINE;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS;
import static io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes.PROCESS_COMMAND_LINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Arrays;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TruncateCommandLineWhenMetricsEnabledTest {

  @Mock ConfigProperties config;

  @Test
  void shouldNotApplyIfNoCommandline() {
    var existing = makeBasicResource(null);

    var testClass = new TruncateCommandLineWhenMetricsEnabled.CommandLineTruncator();

    var result = testClass.apply(existing, config);
    assertSame(existing, result);
  }

  @Test
  void doesntTruncateWhenCommandlineShort() {
    var cmd = "c:\\java.exe runme.jar";
    var existing = makeBasicResource(cmd);

    var testClass = new TruncateCommandLineWhenMetricsEnabled.CommandLineTruncator();
    var result = testClass.apply(existing, config);

    assertSame(existing, result);
    assertThat(result.getAttribute(PROCESS_COMMAND_LINE)).isEqualTo(cmd);
  }

  @Test
  void shouldNotApplyIfConfigItemOverrides() {
    var existing = makeBasicResource("blargus");

    when(config.getBoolean(METRICS_FULL_COMMAND_LINE, false)).thenReturn(true);

    var testClass = new TruncateCommandLineWhenMetricsEnabled.CommandLineTruncator();

    var result = testClass.apply(existing, config);
    assertSame(existing, result);
  }

  @Test
  void truncatesWhenTooLong() {
    var cmd = "foo ".repeat(100).trim();
    var existing = makeBasicResource(cmd);

    var testClass = new TruncateCommandLineWhenMetricsEnabled.CommandLineTruncator();
    var result = testClass.apply(existing, config);

    String resultCmd = result.getAttribute(PROCESS_COMMAND_LINE);
    assertThat(resultCmd.length()).isEqualTo(255);
    assertThat(resultCmd.endsWith("...")).isTrue();
    var cmdArgs = result.getAttribute(PROCESS_COMMAND_ARGS);
    String joinedArgs = String.join(", ", cmdArgs.toArray(new String[] {}));
    assertThat(joinedArgs.length()).isLessThan(255);
    assertThat(joinedArgs.endsWith("...")).isTrue();
    assertThat(result.getAttribute(stringKey("foo"))).isEqualTo("barfly");
  }

  @Test
  void testTruncateThroughSpi() {
    var testClass = new TruncateCommandLineWhenMetricsEnabled();
    var cmd = "foo ".repeat(100).trim();
    var existing = makeBasicResource(cmd);

    var autoConfig = mock(AutoConfigurationCustomizer.class);

    testClass.customize(autoConfig);
    ArgumentCaptor<BiFunction<Resource, ConfigProperties, Resource>> captor =
        ArgumentCaptor.forClass(BiFunction.class);
    verify(autoConfig).addResourceCustomizer(captor.capture());

    var truncator = captor.getValue();

    var result = truncator.apply(existing, config);
    var resultCmd = result.getAttribute(PROCESS_COMMAND_LINE);
    assertThat(resultCmd.length()).isEqualTo(255);
    assertThat(resultCmd.endsWith("...")).isTrue();
    var cmdArgs = result.getAttribute(PROCESS_COMMAND_ARGS);
    String joinedArgs = String.join(", ", cmdArgs.toArray(new String[] {}));
    assertThat(joinedArgs.length()).isLessThan(255);
    assertThat(joinedArgs.endsWith("...")).isTrue();
    assertThat(result.getAttribute(stringKey("foo"))).isEqualTo("barfly");
  }

  @NotNull
  private static Resource makeBasicResource(String cmdLine) {
    return Resource.create(
        Attributes.of(
            PROCESS_COMMAND_LINE,
            cmdLine,
            PROCESS_COMMAND_ARGS,
            cmdLine != null ? Arrays.asList(cmdLine.split(" ")) : null,
            stringKey("foo"),
            "barfly"));
  }
}
