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

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(tags.stream()).isEmpty();
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
                12345L,
                ResourceAttributes.HOST_NAME,
                "astronaut",
                ResourceAttributes.CONTAINER_ID,
                "abcd90210"));

    // when
    var tags = new GlobalTagsBuilder(resource).build();

    // then
    assertThat(tags.stream())
        .hasSize(6)
        .containsExactlyInAnyOrder(
            Tag.of("deployment.environment", "prod"),
            Tag.of("service", "my-service"),
            Tag.of("runtime", "OpenJDK Runtime Environment"),
            Tag.of("process.pid", "12345"),
            Tag.of("host.name", "astronaut"),
            Tag.of("container.id", "abcd90210"));
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
    assertThat(tags.stream())
        .hasSize(1)
        .containsExactly(Tag.of("deployment.environment", "gauntlet"));
  }
}
