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

package com.splunk.opentelemetry.profiler;

import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.exporter.ProfilingEventExporter;
import com.splunk.opentelemetry.profiler.old.AgentInternalsFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class ThreadDumpProcessorBenchmark {

  public static final Path JFR_FILE = Path.of("benchmark.jfr");

  public ThreadDumpProcessorBenchmark() {
    try {
      ensureJfrFileExists();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void ensureJfrFileExists() throws Exception {
    if (Files.exists(JFR_FILE)) {
      return;
    }
    System.out.println("Waiting for subprocess to create JFR file");
    String path = JfrFileMaker.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    String cmd = "java -cp " + path + " " + JfrFileMaker.class.getName() + " " + JFR_FILE + " 60";
    Process exec = Runtime.getRuntime().exec(cmd);
    int rc = exec.waitFor();
    if (rc != 0) {
      System.out.println("rc = " + rc);
      throw new IllegalStateException("NO JFR FILE");
    }
  }

  private static ThreadDumpProcessor buildNewThreadDumpProcessor() {
    SpanContextualizer contextualizer = new SpanContextualizer();
    ProfilingEventExporter profilingEventExporter = x -> {};
    return ThreadDumpProcessor.builder()
        .profilingEventExporter(profilingEventExporter)
        .spanContextualizer(contextualizer)
        .build();
  }

  private static com.splunk.opentelemetry.profiler.old.ThreadDumpProcessor
      buildOldThreadDumpProcessor() {
    SpanContextualizer contextualizer = new SpanContextualizer();
    Consumer<StackToSpanLinkage> processor = x -> {};
    Predicate<String> filter = new AgentInternalsFilter();
    return new com.splunk.opentelemetry.profiler.old.ThreadDumpProcessor(
        contextualizer, processor, filter);
  }

  @Benchmark
  public void newThreadDumpProcessor(RecordingFileState state) {
    state.newThreadDumpProcessor.accept(state.nextEvent());
  }

  @Benchmark
  public void oldThreadDumpProcessor(RecordingFileState state) {
    state.oldThreadDumpProcessor.accept(state.nextEvent());
  }

  @State(Scope.Benchmark)
  public static class RecordingFileState {
    public final ThreadDumpProcessor newThreadDumpProcessor = buildNewThreadDumpProcessor();
    public final com.splunk.opentelemetry.profiler.old.ThreadDumpProcessor oldThreadDumpProcessor =
        buildOldThreadDumpProcessor();

    public static final Path JFR_FILE = Path.of("benchmark.jfr");

    List<RecordedEvent> events = new ArrayList<>();
    int index = 0;

    @Setup(Level.Trial)
    public void setup() throws Exception {
      RecordingFile recordingFile = new RecordingFile(JFR_FILE);
      while (recordingFile.hasMoreEvents()) {
        RecordedEvent event = recordingFile.readEvent();
        if (event.getEventType().getName().equals("jdk.ThreadDump")) {
          events.add(event);
        }
      }
      recordingFile.close();
    }

    public RecordedEvent nextEvent() {
      try {
        return events.get(index++);
      } finally {
        if (index >= events.size()) {
          index = 0;
        }
      }
    }
  }
}
