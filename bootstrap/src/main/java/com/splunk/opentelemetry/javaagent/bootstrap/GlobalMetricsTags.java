package com.splunk.opentelemetry.javaagent.bootstrap;

import io.micrometer.core.instrument.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private GlobalMetricsTags() {}
}
