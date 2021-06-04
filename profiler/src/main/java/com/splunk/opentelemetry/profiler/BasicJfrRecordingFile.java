package com.splunk.opentelemetry.profiler;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.nio.file.Path;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Simple/basic abstraction around a recording file.  Can open and
 * get a stream of events.
 */
public class BasicJfrRecordingFile implements RecordedEventStream {

    private final JFR jfr;

    public BasicJfrRecordingFile(JFR jfr) {
        this.jfr = jfr;
    }

    @Override
    public Stream<RecordedEvent> open(Path path) {
        RecordingFile file = jfr.openRecordingFile(path);
        return StreamSupport.stream(
                new Spliterators.AbstractSpliterator<RecordedEvent>(Long.MAX_VALUE, Spliterator.ORDERED) {
                    public boolean tryAdvance(Consumer<? super RecordedEvent> action) {
                        if (file.hasMoreEvents()) {
                            action.accept(jfr.readEvent(file, path));
                            return true;
                        }
                        return false;
                    }

                    public void forEachRemaining(Consumer<? super RecordedEvent> action) {
                        while (file.hasMoreEvents()) {
                            action.accept(jfr.readEvent(file, path));
                        }
                    }
                }, false);
    }
}
