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

import io.micrometer.core.instrument.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds the list of tags that should be added to all meters registered by the javaagent.
 * It needs to be loaded by the bootstrap classloader to be accessible inside instrumentations.
 */
public final class GlobalMetricsTags {
  private static final Logger log = LoggerFactory.getLogger(GlobalMetricsTags.class);

  private static final List<Tag> EMPTY = Collections.emptyList();
  private static final AtomicReference<List<Tag>> INSTANCE = new AtomicReference<>(EMPTY);

  public static void set(List<Tag> globalTags) {
    List<Tag> globalTagsCopy = Collections.unmodifiableList(new ArrayList<>(globalTags));
    if (!INSTANCE.compareAndSet(EMPTY, globalTagsCopy)) {
      log.warn("GlobalMetricTags#set() was already called before");
    }
  }

  public static List<Tag> get() {
    return INSTANCE.get();
  }

  public static List<Tag> concat(Tag... otherTags) {
    List<Tag> globalTags = get();
    List<Tag> result = new ArrayList<>(globalTags.size() + otherTags.length);
    result.addAll(globalTags);
    result.addAll(Arrays.asList(otherTags));
    return result;
  }

  private GlobalMetricsTags() {}
}
