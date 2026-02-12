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
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class YamlParserTest {
  @Test
  void testBasicRuleParsesOK() throws Exception {
    String yaml = "- class: someClass\n" + "  method: someMethod\n";
    assertEquals(1, YamlParser.parseFromString(yaml).size());
  }

  // formatting of the strings with newlines makes it easier to read and
  // reason about the test.
  // spotless:off
  @ParameterizedTest
  @ValueSource(
      strings = {
        "class: fooButNoWrappingYamlList\n"
            + "  method: thisWillNotWork",
        "- class:\n"
            + "    - name: cannotUseMultipleNameClauses\n"
            + "    - name_regex: withoutAnAndOrOr.*\n"
            + "  method: someMethodName",
        "- class:\n"
            + "    parameter_count: 1\n"
            + "    method: someMethodName\n",
        "- class:\n"
            + "    parameter:\n"
            + "      index: 0\n"
            + "      type: int\n"
            + "  method: someMethodName\n",
        "- class:\n"
            + "    and:\n"
            + "  method: someMethodName\n", // no clauses in and:
        "- class:\n"
            + "    or:\n"
            + "      - not:\n"
            + "          name: singleRuleExpected\n"
            + "          name_regex: underANot.*\n"
            + "  method: someMethodName\n",
        "- class:\n"
            + "    or:\n"
            + "      - not: bareValueOnlySupportedForSimpleClassNames\n"
            + "  method: someMethodName\n",
        "- class: someClassName\n"
            + "  method:\n"
            + "    super_type: notExpectedForMethodMatcher\n",
        "- class: someClassName\n"
            + "  method:\n"
            + "    parameter_count: notanumber\n",
          "- class: someClassName\n"
              + "  method:\n"
              + "    parameter:\n"
              + "      index: notanumber\n"
              + "      type: int\n",
        "- class: someClassName\n"
            + "  method:\n"
            + "    parameter:\n"
            + "      index: 0\n", // no type
          "- class: someClassName\n"
              + "  method:\n"
              + "    parameter:\n"
              + "      type: int\n", // no index
          "- class: someClassName\n"
              + "  method:\n"
              + "    parameter: bareValueNotSupported\n",
          "- class: someClassName\n"
              + "  method:\n"
              + "    parameter:\n"
              + "      noIndex: 1\n"
              + "      noType: int\n",
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
            file_format: "1.0-rc.3"
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
}
