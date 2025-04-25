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

package com.splunk.opentelemetry.profiler.exporter;

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.FRAME_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.perftools.profiles.ProfileProto.Location;
import com.google.perftools.profiles.ProfileProto.Profile;
import com.google.perftools.profiles.ProfileProto.Sample;
import com.splunk.opentelemetry.profiler.InstrumentationSource;
import com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes;
import com.splunk.opentelemetry.profiler.pprof.PprofUtils;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PprofCpuEventExporterTest {
  private final InMemoryOtelLogger logger = new InMemoryOtelLogger();
  private final PprofCpuEventExporter exporter =
      PprofCpuEventExporter.builder()
          .otelLogger(logger)
          .period(Duration.ofMillis(20))
          .stackDepth(1024)
          .instrumentationSource(InstrumentationSource.SNAPSHOT)
          .build();

  @Test
  void noLogRecordWhenNothingToExport() {
    exporter.flush();
    assertThat(logger.records()).isEmpty();
  }

  @Test
  void allStackFramesAreInPprofStringTable() throws Exception {
    var exception = new RuntimeException();

    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        exception.getStackTrace(),
        Instant.now(),
        "",
        "",
        Duration.ZERO);
    exporter.flush();

    var logRecord = logger.records().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));
    var table = profile.getStringTableList();

    assertThat(table).containsAll(fullyQualifiedMethodNames(exception.getStackTrace()));
  }

  private List<String> fullyQualifiedMethodNames(StackTraceElement[] stackTrace) {
    return Arrays.stream(stackTrace)
        .map(stackFrame -> stackFrame.getClassName() + "." + stackFrame.getMethodName())
        .collect(Collectors.toList());
  }

  @Test
  void reportStackTraceWasTruncated() throws Exception {
    var exception = new RuntimeException();
    var exporter =
        PprofCpuEventExporter.builder()
            .otelLogger(logger)
            .period(Duration.ofMillis(20))
            .stackDepth(exception.getStackTrace().length - 1)
            .instrumentationSource(InstrumentationSource.SNAPSHOT)
            .build();

    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        exception.getStackTrace(),
        Instant.now(),
        "",
        "",
        Duration.ZERO);
    exporter.flush();

    var logRecord = logger.records().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));
    var sample = profile.getSample(0);

    var labels = PprofUtils.toLabelString(sample, profile);
    assertThat(labels)
        .containsEntry(ProfilingSemanticAttributes.THREAD_STACK_TRUNCATED.getKey(), "true");
  }

  @Test
  void includeOnlyTruncatedStackFrames() throws Exception {
    var exception = new RuntimeException();
    var depth = exception.getStackTrace().length - 1;
    var exporter =
        PprofCpuEventExporter.builder()
            .otelLogger(logger)
            .period(Duration.ofMillis(20))
            .stackDepth(depth)
            .instrumentationSource(InstrumentationSource.SNAPSHOT)
            .build();

    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        exception.getStackTrace(),
        Instant.now(),
        "",
        "",
        Duration.ZERO);
    exporter.flush();

    var logRecord = logger.records().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));

    var expectedStackTrace = removeModuleInfo(exception.getStackTrace()).subList(0, depth);
    var reportedStackTrace = toStackTrace(profile.getSample(0), profile);
    for (int i = 0; i < depth; i++) {
      var expectedStackFrame = expectedStackTrace.get(i);
      var actualStackFrame = reportedStackTrace.get(i);
      assertAll(
          () ->
              assertEquals(
                  expectedStackFrame.getClassLoaderName(), actualStackFrame.getClassLoaderName()),
          () -> assertEquals(expectedStackFrame.getModuleName(), actualStackFrame.getModuleName()),
          () ->
              assertEquals(
                  expectedStackFrame.getModuleVersion(), actualStackFrame.getModuleVersion()),
          () -> assertEquals(expectedStackFrame.getClassName(), actualStackFrame.getClassName()),
          () -> assertEquals(expectedStackFrame.getLineNumber(), actualStackFrame.getLineNumber()),
          () -> assertEquals(expectedStackFrame.getMethodName(), actualStackFrame.getMethodName()),
          () -> assertEquals(expectedStackFrame.getFileName(), actualStackFrame.getFileName()));
    }
  }

  @Test
  void allStackFramesIncludedInSample() throws Exception {
    var exception = new RuntimeException();

    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        exception.getStackTrace(),
        Instant.now(),
        "",
        "",
        Duration.ZERO);
    exporter.flush();

    var logRecord = logger.records().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));

    var expectedStackTrace = removeModuleInfo(exception.getStackTrace());
    var reportedStackTrace = toStackTrace(profile.getSample(0), profile);
    assertEquals(expectedStackTrace.size(), reportedStackTrace.size());
    for (int i = 0; i < expectedStackTrace.size(); i++) {
      var expectedStackFrame = expectedStackTrace.get(i);
      var actualStackFrame = reportedStackTrace.get(i);
      assertAll(
          () ->
              assertEquals(
                  expectedStackFrame.getClassLoaderName(), actualStackFrame.getClassLoaderName()),
          () -> assertEquals(expectedStackFrame.getModuleName(), actualStackFrame.getModuleName()),
          () ->
              assertEquals(
                  expectedStackFrame.getModuleVersion(), actualStackFrame.getModuleVersion()),
          () -> assertEquals(expectedStackFrame.getClassName(), actualStackFrame.getClassName()),
          () -> assertEquals(expectedStackFrame.getLineNumber(), actualStackFrame.getLineNumber()),
          () -> assertEquals(expectedStackFrame.getMethodName(), actualStackFrame.getMethodName()),
          () -> assertEquals(expectedStackFrame.getFileName(), actualStackFrame.getFileName()));
    }
  }

  private List<StackTraceElement> toStackTrace(Sample sample, Profile profile) {
    List<StackTraceElement> stackTrace = new ArrayList<>();
    for (var locationId : sample.getLocationIdList()) {
      var location = profile.getLocation(locationId.intValue() - 1);
      stackTrace.add(toStackTrace(location, profile));
    }
    return stackTrace;
  }

  private StackTraceElement toStackTrace(Location location, Profile profile) {
    var line = location.getLine(0);
    var functionId = line.getFunctionId();
    var function = profile.getFunction((int) functionId - 1);
    var fileName = profile.getStringTable((int) function.getFilename());
    var functionName = profile.getStringTable((int) function.getName());

    var declaringClass = functionName.substring(0, functionName.lastIndexOf('.'));
    var methodName = functionName.substring(functionName.lastIndexOf('.') + 1);
    return new StackTraceElement(declaringClass, methodName, fileName, (int) line.getLine());
  }

  private List<StackTraceElement> removeModuleInfo(StackTraceElement[] originalStackTrace) {
    List<StackTraceElement> stackTrace = new ArrayList<>();
    for (var stackFrame : originalStackTrace) {
      stackTrace.add(removeModuleInfo(stackFrame));
    }
    return stackTrace;
  }

  private StackTraceElement removeModuleInfo(StackTraceElement stackFrame) {
    return new StackTraceElement(
        stackFrame.getClassName(),
        stackFrame.getMethodName(),
        valueOrUnknown(stackFrame.getFileName()),
        Math.max(stackFrame.getLineNumber(), 0));
  }

  private String valueOrUnknown(String value) {
    return value == null ? "unknown" : value;
  }

  @Test
  void maintainStackFrameCount() {
    var exception = new RuntimeException();

    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        exception.getStackTrace(),
        Instant.now(),
        "",
        "",
        Duration.ZERO);
    exporter.flush();

    var logRecord = logger.records().get(0);
    assertEquals(exception.getStackTrace().length, logRecord.getAttributes().get(FRAME_COUNT));
  }

  @Test
  void maintainStackFrameCountAcrossMultipleStackTraces() {
    var exception1 = new RuntimeException();
    var exception2 = new IllegalArgumentException();
    var exception3 = new IOException();

    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        exception1.getStackTrace(),
        Instant.now(),
        "",
        "",
        Duration.ZERO);
    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        exception2.getStackTrace(),
        Instant.now(),
        "",
        "",
        Duration.ZERO);
    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        exception3.getStackTrace(),
        Instant.now(),
        "",
        "",
        Duration.ZERO);
    exporter.flush();

    var expectedFrameCount =
        exception1.getStackTrace().length
            + exception2.getStackTrace().length
            + exception3.getStackTrace().length;
    var logRecord = logger.records().get(0);
    assertEquals(expectedFrameCount, logRecord.getAttributes().get(FRAME_COUNT));
  }

  @ParameterizedTest
  @EnumSource(Thread.State.class)
  void includeThreadInformationInSamples(Thread.State state) throws Exception {
    var random = new Random();
    var threadId = new Random().nextLong(10_000);
    var threadName = "thread-name-" + random.nextInt(1000);

    exporter.export(
        threadId,
        threadName,
        state,
        new RuntimeException().getStackTrace(),
        Instant.now(),
        "",
        "",
        Duration.ZERO);
    exporter.flush();

    var logRecord = logger.records().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));
    var sample = profile.getSample(0);

    var labels = PprofUtils.toLabelString(sample, profile);
    assertThat(labels).contains(entry(ProfilingSemanticAttributes.THREAD_ID, threadId));
    assertThat(labels).contains(entry(ProfilingSemanticAttributes.THREAD_NAME, threadName));
    assertThat(labels).contains(entry(ProfilingSemanticAttributes.THREAD_STATE, state.toString()));
  }

  @Test
  void includeTraceIdInformationInSamples() throws Exception {
    var traceId = IdGenerator.random().generateTraceId();

    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        new RuntimeException().getStackTrace(),
        Instant.now(),
        traceId,
        "",
        Duration.ZERO);
    exporter.flush();

    var logRecord = logger.records().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));
    var sample = profile.getSample(0);

    var labels = PprofUtils.toLabelString(sample, profile);
    assertThat(labels).contains(entry(ProfilingSemanticAttributes.TRACE_ID, traceId));
  }

  @Test
  void doNotIncludeInvalidTraceIdsInformationInSamples() throws Exception {
    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        new RuntimeException().getStackTrace(),
        Instant.now(),
        "",
        "",
        Duration.ZERO);
    exporter.flush();

    var logRecord = logger.records().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));
    var sample = profile.getSample(0);

    var labels = PprofUtils.toLabelString(sample, profile);
    assertThat(labels).doesNotContainKey(ProfilingSemanticAttributes.TRACE_ID.getKey());
  }

  @Test
  void includeSpanIdInformationInSamples() throws Exception {
    var spanId = IdGenerator.random().generateSpanId();

    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        new RuntimeException().getStackTrace(),
        Instant.now(),
        "",
        spanId,
        Duration.ZERO);
    exporter.flush();

    var logRecord = logger.records().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));
    var sample = profile.getSample(0);

    var labels = PprofUtils.toLabelString(sample, profile);
    assertThat(labels).contains(entry(ProfilingSemanticAttributes.SPAN_ID, spanId));
  }

  @Test
  void doNotIncludeInvalidSpanIdsInformationInSamples() throws Exception {
    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        new RuntimeException().getStackTrace(),
        Instant.now(),
        "",
        "",
        Duration.ZERO);
    exporter.flush();

    var logRecord = logger.records().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));
    var sample = profile.getSample(0);

    var labels = PprofUtils.toLabelString(sample, profile);
    assertThat(labels).doesNotContainKey(ProfilingSemanticAttributes.SPAN_ID.getKey());
  }

  @Test
  void includeStackTraceTimestampInSamples() throws Exception {
    var time = Instant.now();

    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        new RuntimeException().getStackTrace(),
        time,
        "",
        "",
        Duration.ZERO);
    exporter.flush();

    var logRecord = logger.records().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));
    var sample = profile.getSample(0);

    var labels = PprofUtils.toLabelString(sample, profile);
    assertThat(labels)
        .contains(entry(ProfilingSemanticAttributes.SOURCE_EVENT_TIME, time.toEpochMilli()));
  }

  @Test
  void includeStackTraceDurationInSamples() throws Exception {
    var duration = Duration.ofMillis(33);
    exporter.export(
        1,
        "thread-name",
        Thread.State.RUNNABLE,
        new RuntimeException().getStackTrace(),
        Instant.now(),
        "",
        "",
        duration);
    exporter.flush();

    var logRecord = logger.records().get(0);
    var profile = Profile.parseFrom(PprofUtils.deserialize(logRecord));
    var sample = profile.getSample(0);

    var labels = PprofUtils.toLabelString(sample, profile);
    assertThat(labels)
        .contains(entry(ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD, duration.toMillis()));
  }

  private <T> Map.Entry<String, T> entry(AttributeKey<T> attribute, T value) {
    return Map.entry(attribute.getKey(), value);
  }
}
