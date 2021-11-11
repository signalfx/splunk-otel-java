package com.splunk.opentelemetry.instrumentation.tomcat.metrics;

import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.threads.ResizableExecutor;

final class ResizeableExecutorSuppliers implements Suppliers<ResizableExecutor> {

    @Override
    public Number getCurrentThreads(ResizableExecutor executor) {
        return executor.getPoolSize();
    }

    @Override
    public Number getActiveThreads(ResizableExecutor executor) {
        return executor.getActiveCount();
    }

    @Override
    public Number getCoreThreads(ResizableExecutor executor) {
        return Double.NaN;
    }

    @Override
    public Number getMaxThreads(ResizableExecutor executor) {
        return executor.getMaxThreads();
    }

    @Override
    public Number getSubmittedTasks(ResizableExecutor executor) {
        return Double.NaN;
    }

    @Override
    public Number getCompletedTasks(ResizableExecutor executor) {
        return Double.NaN;
    }
}
