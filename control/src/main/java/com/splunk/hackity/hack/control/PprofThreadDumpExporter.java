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

package com.splunk.hackity.hack.control;

import com.splunk.opentelemetry.profiler.exporter.PprofLogDataExporter;
import com.splunk.opentelemetry.profiler.pprof.Pprof;
import java.lang.management.ThreadInfo;

/** Exports complete {@link ThreadInfo} snapshots as pprof log records. */
public class PprofThreadDumpExporter {
  private final PprofThreadDumpMarshaler marshaler;
  private final PprofLogDataExporter logDataExporter;

  public PprofThreadDumpExporter(PprofLogDataExporter logDataExporter) {
    this(new PprofThreadDumpMarshaler(), logDataExporter);
  }

  PprofThreadDumpExporter(
      PprofThreadDumpMarshaler marshaler, PprofLogDataExporter logDataExporter) {
    this.marshaler = marshaler;
    this.logDataExporter = logDataExporter;
  }

  public void export(ThreadInfo[] threadInfos) {
    Pprof pprof = marshaler.marshal(threadInfos);
    if (pprof.hasSamples()) {
      logDataExporter.export(pprof.serialize(), pprof.frameCount());
    }
  }
}
