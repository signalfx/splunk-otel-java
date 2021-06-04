package com.splunk.opentelemetry.profiler;

import jdk.jfr.consumer.RecordedEvent;

import java.nio.file.Path;

public class EventProcessingChain {

    public void accept(Path path, RecordedEvent event) {
        //NO-OP for now....
    }
}
