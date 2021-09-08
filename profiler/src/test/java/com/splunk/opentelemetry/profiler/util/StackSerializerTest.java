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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import org.junit.jupiter.api.Test;

class StackSerializerTest {

  RecordedFrame frame1 = makeFrame("io.test.MyClass", "action", 123);
  RecordedFrame frame2 = makeFrame("io.test.MyClass", "silver", 456);
  RecordedFrame frame3 = makeFrame("io.test.Framewerk", "root", 66);
  List<RecordedFrame> frames = Arrays.asList(frame1, frame2, frame3);
  List<RecordedFrame> framesWithNullMethod = Arrays.asList(frame1, makeFrameWithNullMethod("io.test.MyClass", 456), frame3);

  @Test
  void serialize() {
    StackSerializer serializer = new StackSerializer();
    RecordedStackTrace stack = mock(RecordedStackTrace.class);

    when(stack.getFrames()).thenReturn(frames);

    String result = serializer.serialize(stack);
    String expected =
        "io.test.MyClass.action:123\n"
            + "io.test.MyClass.silver:456\n"
            + "io.test.Framewerk.root:66";
    assertEquals(expected, result);
  }

  @Test
  void serializeWithNullMethod() {
    StackSerializer serializer = new StackSerializer();
    RecordedStackTrace stack = mock(RecordedStackTrace.class);

    when(stack.getFrames()).thenReturn(framesWithNullMethod);

    String result = serializer.serialize(stack);
    String expected =
            "io.test.MyClass.action:123\n"
                    + "io.test.Framewerk.root:66";
    assertEquals(expected, result);
  }

  @Test
  void limitDepth() {
    StackSerializer serializer = new StackSerializer(2);
    RecordedStackTrace stack = mock(RecordedStackTrace.class);

    when(stack.getFrames()).thenReturn(frames);

    String result = serializer.serialize(stack);
    String expected = "io.test.MyClass.silver:456\n" + "io.test.Framewerk.root:66";
    assertEquals(expected, result);
  }

  private RecordedFrame makeFrame(String typeName, String methodName, int line) {
    RecordedFrame frame = mock(RecordedFrame.class);
    RecordedMethod method = mock(RecordedMethod.class);
    RecordedClass type = mock(RecordedClass.class);
    when(method.getType()).thenReturn(type);
    when(frame.getMethod()).thenReturn(method);
    when(method.getName()).thenReturn(methodName);
    when(frame.getInt("lineNumber")).thenReturn(line);
    when(type.getName()).thenReturn(typeName);
    return frame;
  }

  private RecordedFrame makeFrameWithNullMethod(String typeName, int line) {
    RecordedFrame frame = mock(RecordedFrame.class);
    RecordedClass type = mock(RecordedClass.class);
    when(frame.getMethod()).thenReturn(null);
    when(frame.getInt("lineNumber")).thenReturn(line);
    when(type.getName()).thenReturn(typeName);
    return frame;
  }
}
