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

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_TIME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.THREAD_STATE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.TRACE_ID;

import com.google.perftools.profiles.ProfileProto.Profile;
import com.google.perftools.profiles.ProfileProto.Sample;
import java.util.List;

class PprofTranslator {
  public Profile translateToPprof(List<StackTrace> stackTraces) {
    Pprof pprof = new Pprof();
    for (StackTrace stackTrace : stackTraces) {
      pprof.add(translateToPprofSample(stackTrace, pprof));
    }
    return pprof.build();
  }

  private Sample translateToPprofSample(StackTrace stackTrace, Pprof pprof) {
    Sample.Builder sample = Sample.newBuilder();
    sample.addLabel(pprof.newLabel(THREAD_ID, stackTrace.getThreadId()));
    sample.addLabel(pprof.newLabel(THREAD_NAME, stackTrace.getThreadName()));
    sample.addLabel(
        pprof.newLabel(THREAD_STATE, stackTrace.getThreadState().toString().toLowerCase()));
    sample.addLabel(pprof.newLabel(SOURCE_EVENT_NAME, "snapshot-profiling"));
    sample.addLabel(pprof.newLabel(SOURCE_EVENT_TIME, stackTrace.getTimestamp().toEpochMilli()));
    sample.addLabel(pprof.newLabel(SOURCE_EVENT_PERIOD, stackTrace.getDuration().toMillis()));

    for (StackTraceElement stackFrame : stackTrace.getStackFrames()) {
      sample.addLocationId(pprof.getLocationId(stackFrame));
      //      pprof.incFrameCount();
    }
    sample.addLabel(pprof.newLabel(TRACE_ID, stackTrace.getTraceId()));

    return sample.build();
  }
}
