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
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCType;

class StackSerializerTest {
  private static final int STACK_DEPTH = 128;

  IMCFrame frame1 = makeFrame("io.test.MyClass", "action", 123);
  IMCFrame frame2 = makeFrame("io.test.MyClass", "silver", 456);
  IMCFrame frame3 = makeFrame("io.test.Framewerk", "root", 66);
  List<IMCFrame> frames = Arrays.asList(frame1, frame2, frame3);
  List<IMCFrame> framesWithNullMethod =
      Arrays.asList(frame1, makeFrameWithNullMethod("io.test.MyClass", 456), frame3);

  @Test
  void serialize() {
    StackSerializer serializer = new StackSerializer(STACK_DEPTH);
    IMCStackTrace stack = mock(IMCStackTrace.class);

    when(stack.getFrames()).thenReturn((List) frames);

    String result = serializer.serialize(stack);
    String expected =
        "\tat io.test.MyClass.action(unknown:123)\n"
            + "\tat io.test.MyClass.silver(unknown:456)\n"
            + "\tat io.test.Framewerk.root(unknown:66)";
    assertEquals(expected, result);
  }

  @Test
  void serializeWithNullMethod() {
    StackSerializer serializer = new StackSerializer(STACK_DEPTH);
    IMCStackTrace stack = mock(IMCStackTrace.class);

    when(stack.getFrames()).thenReturn((List) framesWithNullMethod);

    String result = serializer.serialize(stack);
    String expected =
        "\tat io.test.MyClass.action(unknown:123)\n"
            + "\tat unknown.unknown(unknown)\n"
            + "\tat io.test.Framewerk.root(unknown:66)";
    assertEquals(expected, result);
  }

  @Test
  void limitDepth() {
    StackSerializer serializer = new StackSerializer(2);
    IMCStackTrace stack = mock(IMCStackTrace.class);

    when(stack.getFrames()).thenReturn((List) frames);

    String result = serializer.serialize(stack);
    String expected =
        "\tat io.test.MyClass.action(unknown:123)\n" + "\tat io.test.MyClass.silver(unknown:456)";
    assertEquals(expected, result);
  }

  private IMCFrame makeFrame(String typeName, String methodName, int line) {
    IMCFrame frame = mock(IMCFrame.class);
    IMCMethod method = mock(IMCMethod.class);
    IMCType type = mock(IMCType.class);
    when(method.getType()).thenReturn(type);
    when(frame.getMethod()).thenReturn(method);
    when(method.getMethodName()).thenReturn(methodName);
    when(frame.getFrameLineNumber()).thenReturn(line);
    when(type.getFullName()).thenReturn(typeName);
    return frame;
  }

  private IMCFrame makeFrameWithNullMethod(String typeName, int line) {
    IMCFrame frame = mock(IMCFrame.class);
    IMCType type = mock(IMCType.class);
    when(frame.getMethod()).thenReturn(null);
    when(frame.getFrameLineNumber()).thenReturn(line);
    when(type.getFullName()).thenReturn(typeName);
    return frame;
  }
}
