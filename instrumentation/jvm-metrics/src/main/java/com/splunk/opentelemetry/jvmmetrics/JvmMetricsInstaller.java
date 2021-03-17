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

package com.splunk.opentelemetry.jvmmetrics;

import static java.util.Collections.singleton;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.javaagent.bootstrap.GlobalMetricsTags;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import java.util.List;

@AutoService(ComponentInstaller.class)
public class JvmMetricsInstaller implements ComponentInstaller {
  @Override
  public void afterByteBuddyAgent() {
    // TODO: test code
    System.out.printf(
        "Running on Java %s. JVM %s - %s - %s%n",
        System.getProperty("java.version"),
        System.getProperty("java.vm.name"),
        System.getProperty("java.vm.vendor"),
        System.getProperty("java.vm.version"));
    String jfrClassResourceName = "jdk.jfr.Recording".replace('.', '/') + ".class";
    System.out.println(
        jfrClassResourceName
            + " is present: "
            + (JvmMetricsInstaller.class.getClassLoader().getResource(jfrClassResourceName)
                != null));
    System.getProperties().forEach((k, v) -> System.out.println("SYS property " + k + " = " + v));
    System.out.println(System.getProperty(""));

    if (!Config.get()
        .isInstrumentationEnabled(
            singleton("jvm-metrics"),
            Config.get().getBooleanProperty("splunk.metrics.enabled", true))) {
      return;
    }

    List<Tag> tags = GlobalMetricsTags.get();
    new ClassLoaderMetrics(tags).bindTo(Metrics.globalRegistry);
    new JvmGcMetrics(tags).bindTo(Metrics.globalRegistry);
    new JvmMemoryMetrics(tags).bindTo(Metrics.globalRegistry);
    new JvmThreadMetrics(tags).bindTo(Metrics.globalRegistry);
  }
}
