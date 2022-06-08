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

// Includes work from:
/*
 * Copyright 2019 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.instrumentation.jvmmetrics.otel;

import static com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.JvmMemory.getUsageValue;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;

public class OtelJvmMemoryMetrics {
  private static final AttributeKey<String> ID = stringKey("id");
  private static final AttributeKey<String> AREA = stringKey("area");

  public void install() {
    Meter meter = OtelMeterProvider.get();

    for (BufferPoolMXBean bufferPoolBean :
        ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
      Attributes attributes = Attributes.of(ID, bufferPoolBean.getName());

      meter
          .gaugeBuilder("runtime.jvm.buffer.count")
          .setUnit("buffers")
          .setDescription("An estimate of the number of buffers in the pool.")
          .buildWithCallback(
              measurement -> measurement.record(bufferPoolBean.getCount(), attributes));

      meter
          .gaugeBuilder("runtime.jvm.buffer.memory.used")
          .setUnit("bytes")
          .setDescription(
              "An estimate of the memory that the Java virtual machine is using for this buffer pool.")
          .buildWithCallback(
              measurement -> measurement.record(bufferPoolBean.getMemoryUsed(), attributes));

      meter
          .gaugeBuilder("runtime.jvm.buffer.total.capacity")
          .setUnit("bytes")
          .setDescription("An estimate of the total capacity of the buffers in this pool.")
          .buildWithCallback(
              measurement -> measurement.record(bufferPoolBean.getTotalCapacity(), attributes));
    }

    for (MemoryPoolMXBean memoryPoolBean :
        ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class)) {
      String area = MemoryType.HEAP.equals(memoryPoolBean.getType()) ? "heap" : "nonheap";
      Attributes attributes = Attributes.of(ID, memoryPoolBean.getName(), AREA, area);

      meter
          .gaugeBuilder("runtime.jvm.memory.used")
          .setUnit("bytes")
          .setDescription("The amount of used memory.")
          .buildWithCallback(
              measurement ->
                  measurement.record(
                      getUsageValue(memoryPoolBean, MemoryUsage::getUsed), attributes));

      meter
          .gaugeBuilder("runtime.jvm.memory.committed")
          .setUnit("bytes")
          .setDescription(
              "The amount of memory in bytes that is committed for the Java virtual machine to use.")
          .buildWithCallback(
              measurement ->
                  measurement.record(
                      getUsageValue(memoryPoolBean, MemoryUsage::getCommitted), attributes));

      meter
          .gaugeBuilder("runtime.jvm.memory.max")
          .setUnit("bytes")
          .setDescription(
              "The maximum amount of memory in bytes that can be used for memory management.")
          .buildWithCallback(
              measurement ->
                  measurement.record(
                      getUsageValue(memoryPoolBean, MemoryUsage::getMax), attributes));
    }
  }
}
