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

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.resources.Resource;
import javax.annotation.Nullable;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class ResourceHolder implements AutoConfigurationCustomizerProvider {
  @Nullable private static volatile Resource resource;

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addResourceCustomizer(
        (resource, config) -> {
          ResourceHolder.resource = resource;
          return resource;
        });
  }

  public static Resource getResource() {
    if (resource == null) {
      throw new IllegalStateException("Resource not set");
    }
    return resource;
  }
}
