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

import io.micrometer.core.instrument.Tag;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.ArrayList;
import java.util.List;

class GlobalTagsBuilder {
  private final Resource resource;

  GlobalTagsBuilder(Resource resource) {
    this.resource = resource;
  }

  List<Tag> build() {
    List<Tag> globalTags = new ArrayList<>(4);
    // Use deployment.environment if it's there, otherwise fall back to environment
    addTag(globalTags, "deployment.environment", AttributeKey.stringKey("deployment.environment"), AttributeKey.stringKey("environment"));
    addTag(globalTags, "service", ResourceAttributes.SERVICE_NAME);
    addTag(globalTags, "runtime", ResourceAttributes.PROCESS_RUNTIME_NAME);
    addTag(globalTags, "process.pid", ResourceAttributes.PROCESS_PID);
    return globalTags;
  }


  private void addTag(List<Tag> tags, String tagName, AttributeKey<?> ... resourceAttributeKeys) {
    for (AttributeKey<?> key : resourceAttributeKeys) {
      Object value = resource.getAttributes().get(key);
      if (value != null) {
        tags.add(Tag.of(tagName, value.toString()));
        return;
      }
    }
  }
}
