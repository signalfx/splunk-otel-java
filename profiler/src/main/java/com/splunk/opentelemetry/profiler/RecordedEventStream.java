package com.splunk.opentelemetry.profiler;

import jdk.jfr.consumer.RecordedEvent;

import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Tag interface for turning a file path into a stream of JFR RecordedEvent instances.
 */
public interface RecordedEventStream {
    Stream<RecordedEvent> open(Path path);
    interface Factory extends Supplier<RecordedEventStream> {

    }
}
