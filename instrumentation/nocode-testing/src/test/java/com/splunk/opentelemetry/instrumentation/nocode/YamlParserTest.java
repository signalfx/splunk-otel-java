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

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.declarativeconfig.internal.model.OpenTelemetryConfigurationModel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class YamlParserTest {
  @RegisterExtension LogCapturer logs = LogCapturer.create().captureForType(YamlParser.class);

  @Test
  void testBasicRuleParsesOK() throws Exception {
    String yaml = """
        - class: someClass
          method: someMethod
        """;
    assertEquals(1, YamlParser.parseFromString(yaml).size());
  }

  // formatting of the strings with newlines makes it easier to read and
  // reason about the test.
  // spotless:off
  @ParameterizedTest
  @ValueSource(
      strings = {
          """
              class: fooButNoWrappingYamlList
                method: thisWillNotWork""",
          """
              - class:
                  - name: cannotUseMultipleNameClauses
                  - name_regex: withoutAnAndOrOr.*
                method: someMethodName""",
          """
              - class:
                  parameter_count: 1
                  method: someMethodName
              """,
          """
              - class:
                  parameter:
                    index: 0
                    type: int
                method: someMethodName
              """,
          """
              - class:
                  and:
                method: someMethodName
              """, // no clauses in and:
          """
              - class:
                  or:
                    - not:
                        name: singleRuleExpected
                        name_regex: underANot.*
                method: someMethodName
              """,
          """
              - class:
                  or:
                    - not: bareValueOnlySupportedForSimpleClassNames
                method: someMethodName
              """,
          """
              - class: someClassName
                method:
                  super_type: notExpectedForMethodMatcher
              """,
          """
              - class: someClassName
                method:
                  parameter_count: notanumber
              """,
          """
              - class: someClassName
                method:
                  parameter:
                    index: notanumber
                    type: int
              """,
          """
              - class: someClassName
                method:
                  parameter:
                    index: 0
              """, // no type
          """
              - class: someClassName
                method:
                  parameter:
                    type: int
              """, // no index
          """
              - class: someClassName
                method:
                  parameter: bareValueNotSupported
              """,
          """
              - class: someClassName
                method:
                  parameter:
                    noIndex: 1
                    noType: int
              """,
      })
  // spotless:on
  void invalidYamlIsInvalid(String yaml) {
    assertThatThrownBy(() -> YamlParser.parseFromString(yaml)).isInstanceOf(RuntimeException.class);
  }

  @Test
  void shouldParseFromDeclarativeConfigYaml() {
    // given
    var yaml =
        """
            file_format: "1.0"
            instrumentation/development:
              java:
                splunk:
                  no_code:
                    - class: foo.Foo
                      method: foo
                    - class: foo.Foo
                      method: throwSomething
        """;
    OpenTelemetryConfigurationModel model = DeclarativeConfigTestUtil.parse(yaml);
    DeclarativeConfigProperties splunkRoot =
        AutoConfigureUtil.getInstrumentationConfig(model).getStructured("splunk", empty());
    List<DeclarativeConfigProperties> ruleNodes = splunkRoot.getStructuredList("no_code");

    // when
    List<NocodeRules.Rule> rules = YamlParser.parseFromDeclarativeConfig(ruleNodes);

    // then
    assertThat(rules).hasSize(2);
  }

  @Test
  void shouldParseCurrentSpanRuleAndWarnAboutUnsupportedFields() throws Exception {
    String yaml =
        """
            - class: someClass
              method: someMethod
              current_span: true
              span_name: ignoredName
              span_kind: CLIENT
              span_status: ignoredStatus
              attributes:
                - key: key
                  value: this.toString()
            """;

    List<NocodeRules.Rule> rules = YamlParser.parseFromString(yaml);

    assertThat(rules).hasSize(1);
    NocodeRules.Rule rule = rules.getFirst();
    assertThat(rule.isCurrentSpan()).isTrue();
    assertThat(rule.getSpanName()).isNull();
    assertThat(rule.getSpanKind()).isNull();
    assertThat(rule.getSpanStatus()).isNull();

    logs.assertContains("current_span rules do not support span_name; ignoring it");
    logs.assertContains("current_span rules do not support span_kind; ignoring it");
    logs.assertContains("current_span rules do not support span_status; ignoring it");
  }
}
