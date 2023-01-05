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

import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_FULL_COMMAND_LINE;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.PROCESS_COMMAND_LINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TruncateCommandLineWhenMetricsEnabledTest {

  @Mock ConfigProperties config;

  @Test
  void shouldNotApplyIfMetricsNotEnabled() {
    when(config.getBoolean(METRICS_ENABLED_PROPERTY, false)).thenReturn(false);

    var testClass = new TruncateCommandLineWhenMetricsEnabled();

    assertFalse(testClass.shouldApply(config, null));
  }

  @Test
  void shouldApplyWhenMetricsEnabled() {
    var existing = Resource.create(Attributes.of(PROCESS_COMMAND_LINE, "foo"));

    when(config.getBoolean(METRICS_ENABLED_PROPERTY, false)).thenReturn(true);

    var testClass = new TruncateCommandLineWhenMetricsEnabled();
    assertTrue(testClass.shouldApply(config, existing));
  }

  @Test
  void shouldNotApplyIfConfigItemOverrides() {
    when(config.getBoolean(METRICS_ENABLED_PROPERTY, false)).thenReturn(true);
    when(config.getBoolean(METRICS_FULL_COMMAND_LINE, false)).thenReturn(true);

    var testClass = new TruncateCommandLineWhenMetricsEnabled();

    assertFalse(testClass.shouldApply(config, null));
  }

  @Test
  void doesntTruncateIfShort() {
    var cmd = "c:\\java.exe runme.jar";
    var existing = Resource.create(Attributes.of(PROCESS_COMMAND_LINE, cmd));

    when(config.getBoolean(METRICS_ENABLED_PROPERTY, false)).thenReturn(true);

    var testClass = new TruncateCommandLineWhenMetricsEnabled();
    testClass.shouldApply(config, existing);

    Resource result = testClass.createResource(config);
    assertThat(result.getAttribute(PROCESS_COMMAND_LINE)).isEqualTo(cmd);
  }

  @Test
  void truncatesWhenTooLong() {
    var cmd = Stream.generate(() -> "x").limit(500).collect(Collectors.joining());
    var existing = Resource.create(Attributes.of(PROCESS_COMMAND_LINE, cmd));

    when(config.getBoolean(METRICS_ENABLED_PROPERTY, false)).thenReturn(true);

    var testClass = new TruncateCommandLineWhenMetricsEnabled();
    testClass.shouldApply(config, existing);

    Resource result = testClass.createResource(config);
    String resultCmd = result.getAttribute(PROCESS_COMMAND_LINE);
    assertThat(resultCmd.length()).isEqualTo(255);
    assertThat(resultCmd.endsWith("[...]")).isTrue();
  }
}
