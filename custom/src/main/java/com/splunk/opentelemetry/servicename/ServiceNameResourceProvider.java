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

package com.splunk.opentelemetry.servicename;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConditionalResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(ResourceProvider.class)
public class ServiceNameResourceProvider implements ConditionalResourceProvider {
  private static final Logger log = LoggerFactory.getLogger(ServiceNameResourceProvider.class);

  @Override
  public Resource createResource(ConfigProperties config) {
    String serviceName = ServiceNameDetector.detectServiceName();
    if (serviceName != null) {
      log.info("Auto-detected service name '{}'.", serviceName);
      return Resource.create(Attributes.of(SERVICE_NAME, serviceName));
    }
    return Resource.empty();
  }

  @Override
  public boolean shouldApply(ConfigProperties config, Resource existing) {
    return ServiceNameChecker.serviceNameNotConfigured(config, existing);
  }

  @Override
  public int order() {
    // make it run later than the spring boot resource provider (100)
    return 200;
  }
}
