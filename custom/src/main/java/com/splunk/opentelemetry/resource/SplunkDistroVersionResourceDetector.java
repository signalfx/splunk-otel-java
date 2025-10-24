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

import static com.splunk.opentelemetry.resource.SplunkDistroVersionResourceFactory.createResource;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class SplunkDistroVersionResourceDetector implements ComponentProvider<Resource> {
  private static final Resource DISTRO_VERSION_RESOURCE = createResource();

  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  @Override
  public String getName() {
    return "splunk_distro_version";
  }

  @Override
  public Resource create(DeclarativeConfigProperties config) {
    return DISTRO_VERSION_RESOURCE;
  }
}
