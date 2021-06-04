package com.splunk.opentelemetry.profiler;

public class JfrException extends RuntimeException {

    public JfrException(String message, Exception e) {
        super(message, e);
    }
}
