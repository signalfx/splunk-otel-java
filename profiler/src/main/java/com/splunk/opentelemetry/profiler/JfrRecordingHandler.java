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

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.EventCollectionUtil;
import org.openjdk.jmc.flightrecorder.internal.FlightRecordingLoader;
import org.openjdk.jmc.flightrecorder.internal.IChunkLoader;
import org.openjdk.jmc.flightrecorder.internal.IChunkSupplier;
import org.openjdk.jmc.flightrecorder.internal.parser.LoaderContext;

/**
 * Responsible for processing a single jfr recording snapshot. It streams events from the recoding
 * into the processing chain.
 */
class JfrRecordingHandler implements Consumer<InputStream> {

  private static final Logger logger = Logger.getLogger(JfrRecordingHandler.class.getName());
  private final EventProcessingChain eventProcessingChain;

  public JfrRecordingHandler(Builder builder) {
    this.eventProcessingChain = builder.eventProcessingChain;
  }

  @Override
  public void accept(InputStream inputStream) {
    Instant start = Instant.now();
    try {
      IChunkSupplier chunkSupplier = FlightRecordingLoader.createChunkSupplier(inputStream);

      byte[] buffer = new byte[0];
      while (true) {
        LoaderContext context = new LoaderContext(Collections.emptyList(), false);
        IChunkLoader chunkLoader = createChunkLoader(chunkSupplier, context, buffer, true);
        if (chunkLoader == null) {
          // we have parsed all chunks
          break;
        }
        // update buffer to reuse it when parsing the next chunk
        buffer = chunkLoader.call();
        IItemCollection items = EventCollectionUtil.build(context.buildEventArrays());
        items.stream().flatMap(iItems -> iItems.stream()).forEach(eventProcessingChain::accept);
        eventProcessingChain.flushBuffer();
      }
    } catch (Exception exception) {
      logger.log(SEVERE, "Error parsing JFR recording", exception);
    } finally {
      Instant end = Instant.now();
      long timeElapsed = Duration.between(start, end).toMillis();
      if (logger.isLoggable(FINE)) {
        logger.log(FINE, "Processed recording in {1}ms", new Object[] {timeElapsed});
      }
      eventProcessingChain.logEventStats();
    }
  }

  @Nullable
  private static IChunkLoader createChunkLoader(
      IChunkSupplier chunkSupplier,
      LoaderContext context,
      byte[] buffer,
      boolean ignoreTruncatedChunk)
      throws Exception {
    Method createChunkLoader =
        FlightRecordingLoader.class.getDeclaredMethod(
            "createChunkLoader",
            IChunkSupplier.class,
            LoaderContext.class,
            byte[].class,
            boolean.class);
    createChunkLoader.setAccessible(true);
    IChunkLoader chunkLoader =
        (IChunkLoader) createChunkLoader.invoke(null, chunkSupplier, context, buffer, true);
    return chunkLoader;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private EventProcessingChain eventProcessingChain;

    public Builder eventProcessingChain(EventProcessingChain eventProcessingChain) {
      this.eventProcessingChain = eventProcessingChain;
      return this;
    }

    public JfrRecordingHandler build() {
      return new JfrRecordingHandler(this);
    }
  }
}
