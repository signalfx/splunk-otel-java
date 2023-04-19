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

import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.EventCollectionUtil;
import org.openjdk.jmc.flightrecorder.internal.ChunkInfo;
import org.openjdk.jmc.flightrecorder.internal.FlightRecordingLoader;
import org.openjdk.jmc.flightrecorder.internal.IChunkLoader;
import org.openjdk.jmc.flightrecorder.internal.IChunkSupplier;
import org.openjdk.jmc.flightrecorder.internal.parser.LoaderContext;

/**
 * Responsible for processing a single jfr file snapshot. It streams events from the
 * RecordedEventStream into the processing chain and, once complete, calls the onFileFinished
 * callback.
 */
class JfrPathHandler implements Consumer<Path> {

  private static final Logger logger = Logger.getLogger(JfrPathHandler.class.getName());
  private final EventProcessingChain eventProcessingChain;
  private final Consumer<Path> onFileFinished;

  public JfrPathHandler(Builder builder) {
    this.eventProcessingChain = builder.eventProcessingChain;
    this.onFileFinished = builder.onFileFinished;
  }

  @Override
  public void accept(Path path) {
    if (logger.isLoggable(FINE)) {
      logger.log(
          FINE,
          "New jfr file detected: {0} size: {1}",
          new Object[] {path, path.toFile().length()});
    }
    Instant start = Instant.now();
    try {
      RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");
      List<ChunkInfo> allChunks =
          FlightRecordingLoader.readChunkInfo(FlightRecordingLoader.createChunkSupplier(raf));
      IChunkSupplier chunkSupplier = FlightRecordingLoader.createChunkSupplier(raf, allChunks);

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

      onFileFinished.accept(path);
    } catch (Exception exception) {
      logger.log(SEVERE, "Error parsing JFR recording", exception);
    } finally {
      Instant end = Instant.now();
      long timeElapsed = Duration.between(start, end).toMillis();
      if (logger.isLoggable(FINE)) {
        logger.log(FINE, "Processed {0} in {1}ms", new Object[] {path, timeElapsed});
      }
      eventProcessingChain.logEventStats();
    }
  }

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
    private Consumer<Path> onFileFinished;

    public Builder eventProcessingChain(EventProcessingChain eventProcessingChain) {
      this.eventProcessingChain = eventProcessingChain;
      return this;
    }

    public Builder onFileFinished(Consumer<Path> onFileFinished) {
      this.onFileFinished = onFileFinished;
      return this;
    }

    public JfrPathHandler build() {
      return new JfrPathHandler(this);
    }
  }
}
