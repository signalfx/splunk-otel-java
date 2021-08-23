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

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.bootstrap.BootstrapPackagesBuilder;
import io.opentelemetry.javaagent.extension.bootstrap.BootstrapPackagesConfigurer;
import java.util.Arrays;

@AutoService(BootstrapPackagesConfigurer.class)
public class MicrometerBootstrapPackagesProvider implements BootstrapPackagesConfigurer {

  @Override
  public void configure(Config config, BootstrapPackagesBuilder builder) {
    builder.addAll(
        Arrays.asList(
            // IMPORTANT: must be io.micrometer.core, because io.micrometer.signalfx needs to be in
            // the agent classloader
            "io.micrometer.core", "org.HdrHistogram", "org.LatencyUtils"));
  }
}
