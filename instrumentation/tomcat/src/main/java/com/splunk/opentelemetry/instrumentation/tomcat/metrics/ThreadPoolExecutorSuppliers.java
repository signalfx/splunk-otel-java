package com.splunk.opentelemetry.instrumentation.tomcat.metrics;

import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import java.util.concurrent.Executor;

final class ThreadPoolExecutorSuppliers implements Suppliers<ThreadPoolExecutor> {

    @Override
    public Number getCurrentThreads(ThreadPoolExecutor executor) {
        return executor.getPoolSize();
    }

    @Override
    public Number getActiveThreads(ThreadPoolExecutor executor) {
        return executor.getActiveCount();
    }

    @Override
    public Number getCoreThreads(ThreadPoolExecutor executor) {
        return executor.getCorePoolSize();
    }

    @Override
    public Number getMaxThreads(ThreadPoolExecutor executor) {
        return executor.getMaximumPoolSize();
    }

    @Override
    public Number getSubmittedTasks(ThreadPoolExecutor executor) {
        return executor.getSubmittedCount();
    }

    @Override
    public Number getCompletedTasks(ThreadPoolExecutor executor) {
        return executor.getCompletedTaskCount();
    }
}
