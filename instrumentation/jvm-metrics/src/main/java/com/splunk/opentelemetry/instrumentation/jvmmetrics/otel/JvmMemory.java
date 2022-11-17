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

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;

/** This class is copied from micrometer. */
class JvmMemory {

  private JvmMemory() {}

  static boolean isConcurrentPhase(String cause, String name) {
    return "No GC".equals(cause) || "Shenandoah Cycles".equals(name) || "ZGC Cycles".equals(name);
  }

  static boolean isAllocationPool(String name) {
    return name != null
        && (name.endsWith("Eden Space")
            || "Shenandoah".equals(name)
            || "ZHeap".equals(name)
            || name.endsWith("nursery-allocate")
            || name.endsWith("-eden") // "balanced-eden"
            || "JavaHeap".equals(name) // metronome
        );
  }

  static boolean isLongLivedPool(String name) {
    return name != null
        && (name.endsWith("Old Gen")
            || name.endsWith("Tenured Gen")
            || "Shenandoah".equals(name)
            || "ZHeap".equals(name)
            || name.endsWith("balanced-old")
            || name.contains("tenured") // "tenured",
            // "tenured-SOA",
            // "tenured-LOA"
            || "JavaHeap".equals(name) // metronome
        );
  }

  static boolean isHeap(MemoryPoolMXBean memoryPoolBean) {
    return MemoryType.HEAP.equals(memoryPoolBean.getType());
  }
}
