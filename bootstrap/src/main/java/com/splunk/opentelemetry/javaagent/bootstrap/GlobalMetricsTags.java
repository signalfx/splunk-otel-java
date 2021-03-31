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

package com.splunk.opentelemetry.javaagent.bootstrap;

import io.micrometer.core.instrument.Tags;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds the list of tags that should be added to all meters registered by the javaagent.
 * It needs to be loaded by the bootstrap classloader to be accessible inside instrumentations.
 */
public final class GlobalMetricsTags {
  private static final Logger log = LoggerFactory.getLogger(GlobalMetricsTags.class);

  private static final Tags EMPTY = Tags.empty();
  private static final AtomicReference<Tags> INSTANCE = new AtomicReference<>(EMPTY);

  public static void set(Tags globalTags) {
    if (!INSTANCE.compareAndSet(EMPTY, globalTags)) {
      log.warn("GlobalMetricTags#set() was already called before");
    }
  }

  public static Tags get() {
    return INSTANCE.get();
  }

  private GlobalMetricsTags() {}
}
