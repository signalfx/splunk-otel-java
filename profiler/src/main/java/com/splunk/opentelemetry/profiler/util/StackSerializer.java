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

  private final int maxDepth;

  public StackSerializer(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  public String serialize(RecordedStackTrace stack) {
    List<RecordedFrame> frames = stack.getFrames();
    return frames.stream()
        // limit the number of stack frames in case jfr stack depth is greater than our stack depth
        // truncate the bottom stack frames the same way as jfr
        .limit(maxDepth)
        .reduce(
            new StringBuilder(), this::serializeFrame, (sb1, sb2) -> sb1.append("\n").append(sb2))
        .toString();
  }

  private StringBuilder serializeFrame(StringBuilder sb, RecordedFrame frame) {
    RecordedMethod method = frame.getMethod();
    if (method == null) {
      return maybeNewline(sb).append("\tat unknown.unknown(unknown)");
    }
    maybeNewline(sb);
    String className = method.getType().getName();
    String methodName = method.getName();
    int lineNumber = frame.getInt("lineNumber");
    return sb.append("\tat ")
        .append(className)
        .append(".")
        .append(methodName)
        .append("(unknown:")
        .append(lineNumber)
        .append(")");
  }

  private StringBuilder maybeNewline(StringBuilder sb) {
    if (sb.length() > 0) {
      sb.append("\n");
    }
    return sb;
  }
}
