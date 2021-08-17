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

package com.splunk.opentelemetry.profiler.util;

import java.util.List;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;

public class StackSerializer {

  private static final int DEFAULT_MAX_DEPTH = 128;
  private final int maxDepth;

  public StackSerializer() {
    this(DEFAULT_MAX_DEPTH);
  }

  public StackSerializer(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  public String serialize(RecordedStackTrace stack) {
    List<RecordedFrame> frames = stack.getFrames();
    int skipNum = Math.max(0, frames.size() - maxDepth);
    return frames.stream()
        .skip(skipNum)
        .limit(maxDepth)
        .reduce(
            "",
            (s, recordedFrame) -> {
              String frameStr = serializeFrame(recordedFrame);
              return s.length() == 0 ? frameStr : s + "\n" + frameStr;
            },
            (s, s2) -> s + "\n" + s2);
  }

  private String serializeFrame(RecordedFrame frame) {
    RecordedMethod method = frame.getMethod();
    String className = method.getType().getName();
    String methodName = method.getName();
    int lineNumber = frame.getInt("lineNumber");
    return className + "." + methodName + ":" + lineNumber;
  }
}
