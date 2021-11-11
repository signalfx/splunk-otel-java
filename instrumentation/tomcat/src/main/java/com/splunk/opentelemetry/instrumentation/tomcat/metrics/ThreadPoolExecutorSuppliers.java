package com.splunk.opentelemetry.instrumentation.tomcat.metrics;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;

final class ThreadPoolExecutorSuppliers implements Suppliers {

    private final ThreadPoolExecutor executor;

    ThreadPoolExecutorSuppliers(ThreadPoolExecutor executor) {
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
        return executor.getCorePoolSize();
    }

    @Override
    public Number getMaxThreads() {
        return executor.getMaximumPoolSize();
    }

    @Override
    public Number getSubmittedTasks() {
        return executor.getSubmittedCount();
    }

    @Override
    public Number getCompletedTasks() {
        return executor.getCompletedTaskCount();
    }
}
