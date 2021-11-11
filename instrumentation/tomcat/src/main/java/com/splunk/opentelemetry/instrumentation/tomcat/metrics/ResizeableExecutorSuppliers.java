package com.splunk.opentelemetry.instrumentation.tomcat.metrics;

import org.apache.tomcat.util.threads.ResizableExecutor;

final class ResizeableExecutorSuppliers implements Suppliers {

    private final ResizableExecutor executor;

    ResizeableExecutorSuppliers(ResizableExecutor executor) {
        this.executor = executor;
    }

    @Override
    public Number getCurrentThreads() {
        return executor.getPoolSize();
    }

    @Override
    public Number getActiveThreads() {
        return executor.getActiveCount();
    }

    @Override
    public Number getCoreThreads() {
        return Double.NaN;
    }

    @Override
    public Number getMaxThreads() {
        return executor.getMaxThreads();
    }

    @Override
    public Number getSubmittedTasks() {
        return Double.NaN;
    }

    @Override
    public Number getCompletedTasks() {
        return Double.NaN;
    }
}
