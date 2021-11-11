package com.splunk.opentelemetry.instrumentation.tomcat.metrics;

import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

final class ThreadPoolExecutorSuppliers implements Suppliers {

    private final AbstractEndpoint<?, ?> endpoint;

    ThreadPoolExecutorSuppliers(AbstractEndpoint<?, ?> endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public Number getCurrentThreads() {
        return executor().getPoolSize();
    }

    @Override
    public Number getActiveThreads() {
        return executor().getActiveCount();
    }

    @Override
    public Number getCoreThreads() {
        return executor().getCorePoolSize();
    }

    @Override
    public Number getMaxThreads() {
        return executor().getMaximumPoolSize();
    }

    @Override
    public Number getSubmittedTasks() {
        return executor().getSubmittedCount();
    }

    @Override
    public Number getCompletedTasks() {
        return executor().getCompletedTaskCount();
    }

    private ThreadPoolExecutor executor() {
        return (ThreadPoolExecutor) endpoint.getExecutor();
    }
}
