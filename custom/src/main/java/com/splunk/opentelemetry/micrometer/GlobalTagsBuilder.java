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

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.CONTAINER_ID;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.DEPLOYMENT_ENVIRONMENT;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.HOST_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.PROCESS_PID;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.PROCESS_RUNTIME_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.List;

class GlobalTagsBuilder {
  private final Resource resource;

  GlobalTagsBuilder(Resource resource) {
    this.resource = resource;
  }

  Tags build() {
    List<Tag> globalTags = new ArrayList<>(4);
    // Use deployment.environment if it's there, otherwise fall back to environment
    addTag(
        globalTags,
        "deployment.environment",
        DEPLOYMENT_ENVIRONMENT,
        AttributeKey.stringKey("environment"));
    addTag(globalTags, "service", SERVICE_NAME);
    addTag(globalTags, "runtime", PROCESS_RUNTIME_NAME);
    addTag(globalTags, PROCESS_PID.getKey(), PROCESS_PID);
    addTag(globalTags, CONTAINER_ID.getKey(), CONTAINER_ID);
    addTag(globalTags, HOST_NAME.getKey(), HOST_NAME);
    return Tags.of(globalTags);
  }

  private void addTag(List<Tag> tags, String tagName, AttributeKey<?>... resourceAttributeKeys) {
    for (AttributeKey<?> key : resourceAttributeKeys) {
      Object value = resource.getAttributes().get(key);
      if (value != null) {
        tags.add(Tag.of(tagName, value.toString()));
        return;
      }
    }
  }
}
