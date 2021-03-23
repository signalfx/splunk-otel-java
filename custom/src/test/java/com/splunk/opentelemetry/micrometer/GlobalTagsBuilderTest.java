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

package com.splunk.opentelemetry.micrometer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.Tag;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.junit.jupiter.api.Test;

class GlobalTagsBuilderTest {
  @Test
  void shouldBuildEmptyTagsList() {
    // given
    var resource = Resource.create(Attributes.empty());

    // when
    var tags = new GlobalTagsBuilder(resource).build();

    // then
    assertTrue(tags.isEmpty());
  }

  @Test
  void shouldBuildGlobalTagsList() {
    // given
    var resource =
        Resource.create(
            Attributes.of(
                AttributeKey.stringKey("environment"),
                "prod",
                ResourceAttributes.SERVICE_NAME,
                "my-service",
                ResourceAttributes.PROCESS_RUNTIME_NAME,
                "OpenJDK Runtime Environment",
                ResourceAttributes.PROCESS_PID,
                12345L));

    // when
    var tags = new GlobalTagsBuilder(resource).build();

    // then
    assertEquals(4, tags.size());
    assertEquals(Tag.of("deployment.environment", "prod"), tags.get(0));
    assertEquals(Tag.of("service", "my-service"), tags.get(1));
    assertEquals(Tag.of("runtime", "OpenJDK Runtime Environment"), tags.get(2));
    assertEquals(Tag.of("process.pid", "12345"), tags.get(3));
  }

  @Test
  void preferDeploymentEnvironment() {
    // given
    var resource =
        Resource.create(
            Attributes.of(
                AttributeKey.stringKey("deployment.environment"),
                "gauntlet",
                AttributeKey.stringKey("environment"),
                "oldstyle"));

    // when
    var tags = new GlobalTagsBuilder(resource).build();

    // then
    assertEquals(1, tags.size());
    assertEquals(Tag.of("deployment.environment", "gauntlet"), tags.get(0));
  }
}
