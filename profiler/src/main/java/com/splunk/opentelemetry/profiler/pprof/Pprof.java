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

package com.splunk.opentelemetry.profiler.pprof;

import static com.google.perftools.profiles.ProfileProto.*;

import com.splunk.opentelemetry.profiler.Configuration;
import io.opentelemetry.api.common.AttributeKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

public class Pprof {
  private final Profile.Builder profileBuilder = Profile.newBuilder();
  private final StringTable stringTable = new StringTable(profileBuilder);
  private final FunctionTable functionTable = new FunctionTable(profileBuilder, stringTable);
  private final LocationTable locationTable = new LocationTable(profileBuilder, functionTable);
  private int frameCount;

  public Profile.Builder getProfileBuilder() {
    return profileBuilder;
  }

  public long getStringId(String str) {
    return stringTable.get(str);
  }

  public long getLocationId(String file, String function, long line) {
    return locationTable.get(file, function, line);
  }

  public void addLabel(Sample.Builder sample, AttributeKey<String> key, String value) {
    addLabel(sample, key.getKey(), value);
  }

  public void addLabel(Sample.Builder sample, AttributeKey<Boolean> key, boolean value) {
    addLabel(sample, key.getKey(), Boolean.toString(value));
  }

  public void addLabel(Sample.Builder sample, String name, String value) {
    if (value == null) {
      return;
    }
    addLabel(sample, name, label -> label.setStr(stringTable.get(value)));
  }

  public void addLabel(Sample.Builder sample, AttributeKey<Long> key, long value) {
    addLabel(sample, key.getKey(), value);
  }

  public void addLabel(Sample.Builder sample, String name, long value) {
    addLabel(sample, name, label -> label.setNum(value));
  }

  private void addLabel(Sample.Builder sample, String name, Consumer<Label.Builder> valueSetter) {
    Label.Builder label = Label.newBuilder();
    label.setKey(stringTable.get(name));
    valueSetter.accept(label);
    sample.addLabel(label.build());
  }

  public boolean hasSamples() {
    return profileBuilder.getSampleCount() > 0;
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

  public byte[] serialize(Configuration.DataFormat dataFormat) {
    if (dataFormat != Configuration.DataFormat.PPROF_GZIP_BASE64) {
      throw new IllegalArgumentException("Unsupported data format " + dataFormat);
    }

    Profile profile = profileBuilder.build();
    try {
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      try (OutputStream outputStream = new GZIPOutputStream(byteStream)) {
        profile.writeTo(outputStream);
      }
      byte[] bytes = byteStream.toByteArray();
      return Base64.getEncoder().encode(bytes);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to serialize pprof", exception);
    }
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
      return table.computeIfAbsent(
          functionKey,
          key -> {
            Function fn =
                Function.newBuilder()
                    .setId(index)
                    .setFilename(stringTable.get(key.file))
                    .setName(stringTable.get(key.function))
                    .build();
            profile.addFunction(fn);
            return index++;
          });
    }
  }

  private static class FunctionKey {
    private final String file;
    private final String function;

    FunctionKey(String file, String function) {
      this.file = file;
      this.function = function;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FunctionKey that = (FunctionKey) o;
      return Objects.equals(file, that.file) && Objects.equals(function, that.function);
    }

    @Override
    public int hashCode() {
      return Objects.hash(file, function);
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

    long get(String file, String function, long line) {
      FunctionKey functionKey = new FunctionKey(file, function);
      LocationKey locationKey = new LocationKey(functionKey, line);
      return table.computeIfAbsent(
          locationKey,
          key -> {
            Location location =
                Location.newBuilder()
                    .setId(index)
                    .addLine(
                        Line.newBuilder()
                            .setFunctionId(functionTable.get(functionKey))
                            .setLine(line)
                            .build())
                    .build();
            profile.addLocation(location);
            return index++;
          });
    }
  }

  private static class LocationKey {
    private final FunctionKey functionKey;
    private final long line;

    LocationKey(FunctionKey functionKey, long line) {
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
}
