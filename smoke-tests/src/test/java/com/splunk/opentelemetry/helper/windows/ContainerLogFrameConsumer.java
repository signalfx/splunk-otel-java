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

package com.splunk.opentelemetry.helper.windows;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Frame;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ContainerLogFrameConsumer
    extends ResultCallbackTemplate<ContainerLogFrameConsumer, Frame>
    implements ContainerLogHandler {
  private final List<Listener> listeners;

  public ContainerLogFrameConsumer() {
    this.listeners = new ArrayList<>();
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void onNext(Frame frame) {
    LineType lineType = getLineType(frame);

    if (lineType != null) {
      byte[] bytes = frame.getPayload();
      String text = bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);

      for (Listener listener : listeners) {
        listener.accept(lineType, text);
      }
    }
  }

  private LineType getLineType(Frame frame) {
    switch (frame.getStreamType()) {
      case STDOUT:
        return LineType.STDOUT;
      case STDERR:
        return LineType.STDERR;
      default:
        return null;
    }
  }
}
