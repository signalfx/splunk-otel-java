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

package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.perftools.profiles.ProfileProto.Location;
import com.google.perftools.profiles.ProfileProto.Profile;
import com.google.perftools.profiles.ProfileProto.Sample;
import com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PprofTranslatorTest {
  private final PprofTranslator translator = new PprofTranslator();

  @Test
  void allStackFramesAreInPprofStringTable() {
    var exception = new RuntimeException();
    var stackTrace = Snapshotting.stackTrace().with(exception).build();

    var profile = translator.toPprof(List.of(stackTrace)).build();
    var table = profile.getStringTableList();

    assertThat(table).containsAll(fullyQualifiedMethodNames(exception.getStackTrace()));
  }

  private List<String> fullyQualifiedMethodNames(StackTraceElement[] stackTrace) {
    return Arrays.stream(stackTrace)
        .map(stackFrame -> stackFrame.getClassName() + "." + stackFrame.getMethodName())
        .collect(Collectors.toList());
  }

  @Test
  void allStackFramesIncludedInSample() {
    var exception = new RuntimeException();
    var stackTrace = Snapshotting.stackTrace().with(exception).build();

    var profile = translator.toPprof(List.of(stackTrace)).build();

    var expectedStackTrace = removeModuleInfo(exception.getStackTrace());
    var reportedStackTrace = toStackTrace(profile.getSample(0), profile);
    assertEquals(expectedStackTrace.size(), reportedStackTrace.size());
    for (int i = 0; i < expectedStackTrace.size(); i++) {
      var expectedStackFrame = expectedStackTrace.get(0);
      var actualStackFrame = reportedStackTrace.get(0);
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
        stackFrame.getFileName(),
        stackFrame.getLineNumber());
  }

  @Test
  void maintainStackFrameCount() {
    var stackTrace = Snapshotting.stackTrace().with(new RuntimeException()).build();

    var pprof = translator.toPprof(List.of(stackTrace));

    assertEquals(stackTrace.getStackFrames().length, pprof.frameCount());
  }

  @Test
  void maintainStackFrameCountAcrossMultipleStackTraces() {
    var stackTrace1 = Snapshotting.stackTrace().with(new RuntimeException()).build();
    var stackTrace2 = Snapshotting.stackTrace().with(new IllegalArgumentException()).build();
    var stackTrace3 = Snapshotting.stackTrace().with(new IOException()).build();

    var pprof = translator.toPprof(List.of(stackTrace1, stackTrace2, stackTrace3));

    var expectedFrameCount =
        stackTrace1.getStackFrames().length
            + stackTrace2.getStackFrames().length
            + stackTrace3.getStackFrames().length;
    assertEquals(expectedFrameCount, pprof.frameCount());
  }

  @Test
  void includeThreadInformationInSamples() {
    var stackTrace = Snapshotting.stackTrace().build();

    var profile = translator.toPprof(List.of(stackTrace)).build();
    var sample = profile.getSample(0);

    var labels = toLabelString(sample, profile);
    assertThat(labels)
        .containsEntry(ProfilingSemanticAttributes.THREAD_ID.getKey(), stackTrace.getThreadId());
    assertThat(labels)
        .containsEntry(
            ProfilingSemanticAttributes.THREAD_NAME.getKey(), stackTrace.getThreadName());
    assertThat(labels)
        .containsEntry(
            ProfilingSemanticAttributes.THREAD_STATE.getKey(),
            stackTrace.getThreadState().toString().toLowerCase());
  }

  @Test
  void includeTraceIdInformationInSamples() {
    var stackTrace = Snapshotting.stackTrace().build();

    var profile = translator.toPprof(List.of(stackTrace)).build();
    var sample = profile.getSample(0);

    var labels = toLabelString(sample, profile);
    assertThat(labels)
        .containsEntry(ProfilingSemanticAttributes.TRACE_ID.getKey(), stackTrace.getTraceId());
  }

  @Test
  void includeSourceEventNameAsSnapshotProfilingInSamples() {
    var stackTrace = Snapshotting.stackTrace().build();

    var profile = translator.toPprof(List.of(stackTrace)).build();
    var sample = profile.getSample(0);

    var labels = toLabelString(sample, profile);
    assertThat(labels)
        .containsEntry(
            ProfilingSemanticAttributes.SOURCE_EVENT_NAME.getKey(), "snapshot-profiling");
  }

  @Test
  void includeStackTraceTimestampInSamples() {
    var stackTrace = Snapshotting.stackTrace().build();

    var profile = translator.toPprof(List.of(stackTrace)).build();
    var sample = profile.getSample(0);

    var labels = toLabelString(sample, profile);
    assertThat(labels)
        .containsEntry(
            ProfilingSemanticAttributes.SOURCE_EVENT_TIME.getKey(),
            stackTrace.getTimestamp().toEpochMilli());
  }

  @Test
  void includeStackTraceDurationInSamples() {
    var stackTrace = Snapshotting.stackTrace().build();

    var profile = translator.toPprof(List.of(stackTrace)).build();
    var sample = profile.getSample(0);

    var labels = toLabelString(sample, profile);
    assertThat(labels)
        .containsEntry(
            ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD.getKey(),
            stackTrace.getDuration().toMillis());
  }

  private Map<String, Object> toLabelString(Sample sample, Profile profile) {
    var labels = new HashMap<String, Object>();
    for (var label : sample.getLabelList()) {
      var stringTableIndex = label.getKey();
      var key = profile.getStringTable((int) stringTableIndex);
      if (label.getStr() > 0) {
        labels.put(key, profile.getStringTable((int) label.getStr()));
      } else {
        labels.put(key, label.getNum());
      }
    }
    return labels;
  }
}
