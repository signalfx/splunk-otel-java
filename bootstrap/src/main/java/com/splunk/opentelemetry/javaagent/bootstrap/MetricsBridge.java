package com.splunk.opentelemetry.javaagent.bootstrap;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MetricsBridge {
  public static Counter counter(String name, Tag... tags) {
    List<Tag> globalTags = GlobalMetricsTags.get();
    List<Tag> allTags = new ArrayList<>(globalTags.size() + tags.length);
    allTags.addAll(globalTags);
    allTags.addAll(Arrays.asList(tags));
    return Metrics.counter(name, allTags);
  }

  private MetricsBridge() {}
}
