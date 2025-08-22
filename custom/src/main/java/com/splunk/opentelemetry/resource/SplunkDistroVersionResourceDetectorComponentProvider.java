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

import static io.opentelemetry.semconv.incubating.TelemetryIncubatingAttributes.TELEMETRY_DISTRO_VERSION;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class SplunkDistroVersionResourceDetectorComponentProvider
    implements ComponentProvider<Resource> {
  private static final Logger logger =
      Logger.getLogger(SplunkDistroVersionResourceDetectorComponentProvider.class.getName());

  private final AtomicReference<Resource> resourceReference = new AtomicReference<>();

  private static final String SPLUNK_PROPERTIES = "splunk.properties";

  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  // TODO: Add to Splunk specific yaml config file format documentation
  @Override
  public String getName() {
    return "splunk_distro_version";
  }

  @Override
  public Resource create(DeclarativeConfigProperties config) {
    if (resourceReference.get() == null) {
      synchronized (resourceReference) {
        if (resourceReference.get() == null) {
          resourceReference.set(createResource());
        }
      }
    }
    return resourceReference.get();
  }

  @VisibleForTesting
  static Resource createResource(InputStream inputStream) throws IOException {
    if (inputStream == null) {
      return Resource.empty();
    }

    Properties splunkProps = new Properties();
    splunkProps.load(inputStream);
    return Resource.create(
        Attributes.of(
            TELEMETRY_DISTRO_VERSION, splunkProps.getProperty(TELEMETRY_DISTRO_VERSION.getKey())));
  }

  private static Resource createResource() {
    try (InputStream in =
        ResourceProvider.class.getClassLoader().getResourceAsStream(SPLUNK_PROPERTIES)) {
      return createResource(in);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to load " + SPLUNK_PROPERTIES, e);
      return Resource.empty();
    }
  }
}
