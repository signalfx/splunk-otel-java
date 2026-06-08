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

package com.splunk.opentelemetry.opamp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class YamlNodeBuilderTest {
  @Test
  @SuppressWarnings("unchecked")
  void addNestedNode() {
    Map<String, Object> level1 = YamlNodeBuilder.createNestedNode("abc.def.ghi", 123);

    assertThat(level1).hasSize(1);
    assertThat(level1).containsKey("abc");
    assertThat(level1.get("abc")).isInstanceOf(Map.class);

    Map<String, Object> level2 = (Map<String, Object>) level1.get("abc");

    assertThat(level2).hasSize(1);
    assertThat(level2).containsKey("def");
    assertThat(level2.get("def")).isInstanceOf(Map.class);

    Map<String, Object> level3 = (Map<String, Object>) level2.get("def");

    assertThat(level3).hasSize(1);
    assertThat(level3).containsKey("ghi");
    assertThat(level3.get("ghi")).isEqualTo(123);
  }

  @Test
  void addNodeWithChildBuilder() {
    Map<String, Object> root =
        YamlNodeBuilder.create()
            .addNode(
                "distribution",
                distribution ->
                    distribution.addNode(
                        "splunk",
                        splunk ->
                            splunk.addNode(
                                "profiling",
                                profiling ->
                                    profiling
                                        .addNestedNode(
                                            "always_on.cpu_profiler.sampling_interval", 1410)
                                        .addNestedNode("callgraphs.sampling_interval", 10))))
            .build();

    Map<String, Object> distribution = node(root, "distribution");
    Map<String, Object> splunk = node(distribution, "splunk");
    Map<String, Object> profiling = node(splunk, "profiling");
    Map<String, Object> alwaysOn = node(profiling, "always_on");
    Map<String, Object> cpuProfiler = node(alwaysOn, "cpu_profiler");
    Map<String, Object> callgraphs = node(profiling, "callgraphs");

    assertThat(cpuProfiler).containsEntry("sampling_interval", 1410);
    assertThat(callgraphs).containsEntry("sampling_interval", 10);
  }

  @Test
  void skipsEmptyChildBuilder() {
    Map<String, Object> root =
        YamlNodeBuilder.create()
            .addNode(
                "parent",
                parent -> {
                  parent.addNode("present", "yes");
                })
            .addNode("empty_parent", parent -> {})
            .addNestedNode("nested.empty_parent", parent -> {})
            .build();

    assertThat(root).containsOnlyKeys("parent");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> node(Map<String, Object> node, String key) {
    return (Map<String, Object>) node.get(key);
  }
}
