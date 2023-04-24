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

import static org.openjdk.jmc.common.item.Attribute.attr;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.THREAD_DUMP_RESULT;

import java.time.Instant;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.parser.v1.ThreadUtil;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;

public class EventReader {
  private static final IAttribute<String> EVENT_TRACE_ID = attr("traceId", "trace id", PLAIN_TEXT);
  private static final IAttribute<String> EVENT_SPAN_ID = attr("spanId", "span id", PLAIN_TEXT);
  private static final IAttribute<IQuantity> EVENT_TRACE_FLAGS =
      attr("traceFlags", "trace flags", NUMBER);

  public Instant getStartInstant(IItem event) {
    return Instant.ofEpochSecond(0, getStartTime(event));
  }

  public long getStartTime(IItem event) {
    IMemberAccessor<IQuantity, IItem> accessor =
        getItemType(event).getAccessor(JfrAttributes.START_TIME.getKey());
    return accessor.getMember(event).longValue();
  }

  public String getThreadDumpResult(IItem event) {
    IMemberAccessor<String, IItem> accessor =
        getItemType(event).getAccessor(THREAD_DUMP_RESULT.getKey());

    return accessor.getMember(event);
  }

  public IMCThread getThread(IItem event) {
    IMemberAccessor<IMCThread, IItem> accessor =
        getItemType(event).getAccessor(JfrAttributes.EVENT_THREAD.getKey());
    return accessor.getMember(event);
  }

  public IMCStackTrace getStackTrace(IItem event) {
    IMemberAccessor<IMCStackTrace, IItem> accessor =
        getItemType(event).getAccessor(JfrAttributes.EVENT_STACKTRACE.getKey());
    return accessor.getMember(event);
  }

  public String getTraceId(IItem event) {
    IMemberAccessor<String, IItem> accessor =
        getItemType(event).getAccessor(EVENT_TRACE_ID.getKey());
    return accessor.getMember(event);
  }

  public String getSpanId(IItem event) {
    IMemberAccessor<String, IItem> accessor =
        getItemType(event).getAccessor(EVENT_SPAN_ID.getKey());
    return accessor.getMember(event);
  }

  public byte getTraceFlags(IItem event) {
    IMemberAccessor<IQuantity, IItem> accessor =
        getItemType(event).getAccessor(EVENT_TRACE_FLAGS.getKey());
    return accessor.getMember(event).numberValue().byteValue();
  }

  public long getAllocationSize(IItem event) {
    IMemberAccessor<IQuantity, IItem> accessor =
        getItemType(event).getAccessor(JdkAttributes.ALLOCATION_SIZE.getKey());
    return accessor.getMember(event).longValue();
  }

  public long getSampleWeight(IItem event) {
    IMemberAccessor<IQuantity, IItem> accessor =
        getItemType(event).getAccessor(JdkAttributes.SAMPLE_WEIGHT.getKey());
    return accessor.getMember(event).longValue();
  }

  public long getOSThreadId(IMCThread thread) {
    Long value = ThreadUtil.getOsThreadId(thread);
    return value != null ? value.longValue() : -1;
  }

  private static IType<IItem> getItemType(IItem item) {
    return (IType<IItem>) item.getType();
  }
}
