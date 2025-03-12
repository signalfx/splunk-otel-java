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

import static com.google.perftools.profiles.ProfileProto.Function;
import static com.google.perftools.profiles.ProfileProto.Label;
import static com.google.perftools.profiles.ProfileProto.Line;
import static com.google.perftools.profiles.ProfileProto.Location;
import static com.google.perftools.profiles.ProfileProto.Profile;
import static com.google.perftools.profiles.ProfileProto.Sample;

import io.opentelemetry.api.common.AttributeKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Adapted from the Splunk OpenTelemetry profiler <a
 * href=https://github.com/signalfx/splunk-otel-java/blob/main/profiler/src/main/java/com/splunk/opentelemetry/profiler/pprof/Pprof.java>here</a>,
 * which is itself adapted from Google's Bazel build system <a
 * href=https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/profiler/memory/AllocationTracker.java>here</a>.
 */
class Pprof {
  private final Profile.Builder profileBuilder = Profile.newBuilder();
  private final StringTable stringTable = new StringTable(profileBuilder);
  private final FunctionTable functionTable = new FunctionTable(profileBuilder, stringTable);
  private final LocationTable locationTable = new LocationTable(profileBuilder, functionTable);
  private int frameCount;

  Profile build() {
    return profileBuilder.build();
  }

  void add(Sample sample) {
    profileBuilder.addSample(sample);
  }

  long getLocationId(StackTraceElement stackFrame) {
    return locationTable.get(stackFrame);
  }

  Label newLabel(AttributeKey<String> key, String value) {
    return newLabel(key.getKey(), label -> label.setStr(stringTable.get(value)));
  }

  Label newLabel(AttributeKey<Long> key, long value) {
    return newLabel(key.getKey(), value);
  }

  Label newLabel(String key, long value) {
    return newLabel(key, label -> label.setNum(value));
  }

  private Label newLabel(String name, Consumer<Label.Builder> valueSetter) {
    Label.Builder label = Label.newBuilder();
    label.setKey(stringTable.get(name));
    valueSetter.accept(label);
    return label.build();
  }

  public void incFrameCount() {
    frameCount++;
  }

  /**
   * @return non unique stack frames in this pprof batch
   */
  public int frameCount() {
    return frameCount;
  }

  // copied from
  // https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/profiler/memory/AllocationTracker.java
  private static class StringTable {
    final Profile.Builder profile;
    final Map<String, Long> table = new HashMap<>();
    long index = 0;

    StringTable(Profile.Builder profile) {
      this.profile = profile;
      get(""); // 0 is reserved for the empty string
    }

    long get(String str) {
      return table.computeIfAbsent(
          str,
          key -> {
            profile.addStringTable(key);
            return index++;
          });
    }
  }

  private static class LocationTable {
    final Profile.Builder profile;
    final FunctionTable functionTable;
    final Map<LocationKey, Long> table = new HashMap<>();
    long index = 1; // 0 is reserved

    LocationTable(Profile.Builder profile, FunctionTable functionTable) {
      this.profile = profile;
      this.functionTable = functionTable;
    }

    long get(StackTraceElement stackFrame) {
      LocationKey locationKey = LocationKey.from(stackFrame);
      Location location =
          Location.newBuilder()
              .setId(index)
              .addLine(
                  Line.newBuilder()
                      .setFunctionId(functionTable.get(locationKey.functionKey))
                      .setLine(locationKey.line))
              .build();
      return table.computeIfAbsent(
          locationKey,
          key -> {
            profile.addLocation(location);
            return index++;
          });
    }
  }

  private static class LocationKey {
    private final FunctionKey functionKey;
    private final long line;

    static LocationKey from(StackTraceElement stackFrame) {
      return new LocationKey(FunctionKey.from(stackFrame), stackFrame.getLineNumber());
    }

    private LocationKey(FunctionKey functionKey, long line) {
      this.functionKey = functionKey;
      this.line = line;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      LocationKey that = (LocationKey) o;
      return line == that.line && Objects.equals(functionKey, that.functionKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(functionKey, line);
    }
  }

  private static class FunctionTable {
    final Profile.Builder profile;
    final StringTable stringTable;
    final Map<FunctionKey, Long> table = new HashMap<>();
    long index = 1; // 0 is reserved

    FunctionTable(Profile.Builder profile, StringTable stringTable) {
      this.profile = profile;
      this.stringTable = stringTable;
    }

    long get(FunctionKey functionKey) {
      Function fn =
          Function.newBuilder()
              .setId(index)
              .setFilename(stringTable.get(functionKey.file))
              .setName(stringTable.get(functionKey.className + "." + functionKey.function))
              .build();
      return table.computeIfAbsent(
          functionKey,
          key -> {
            profile.addFunction(fn);
            return index++;
          });
    }
  }

  private static class FunctionKey {
    private final String file;
    private final String className;
    private final String function;

    static FunctionKey from(StackTraceElement stackFrame) {
      return new FunctionKey(
          stackFrame.getFileName() == null ? "Unknown Source" : stackFrame.getFileName(),
          stackFrame.getClassName(),
          stackFrame.getMethodName());
    }

    private FunctionKey(String file, String className, String function) {
      this.file = file;
      this.className = className;
      this.function = function;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FunctionKey that = (FunctionKey) o;
      return Objects.equals(file, that.file)
          && Objects.equals(className, that.className)
          && Objects.equals(function, that.function);
    }

    @Override
    public int hashCode() {
      return Objects.hash(file, className, function);
    }
  }
}
