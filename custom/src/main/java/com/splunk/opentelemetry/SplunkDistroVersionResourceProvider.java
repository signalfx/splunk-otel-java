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

import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@AutoService(ResourceProvider.class)
public class SplunkDistroVersionResourceProvider implements ResourceProvider {
  static final AttributeKey<String> SPLUNK_DISTRO_VERSION =
      AttributeKey.stringKey("splunk.distro.version");

  private static final Resource DISTRO_VERSION_RESOURCE = initialize();

  private static Resource initialize() {
    InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("splunk.properties");
    if (in == null) {
      return Resource.empty();
    }
    try (InputStream ignored = in) {
      Properties splunkProps = new Properties();
      splunkProps.load(in);
      return Resource.create(
          Attributes.of(
              SPLUNK_DISTRO_VERSION, splunkProps.getProperty(SPLUNK_DISTRO_VERSION.getKey())));
    } catch (IOException e) {
      return Resource.empty();
    }
  }

  @Override
  public Resource createResource(ConfigProperties config) {
    return DISTRO_VERSION_RESOURCE;
  }
}
