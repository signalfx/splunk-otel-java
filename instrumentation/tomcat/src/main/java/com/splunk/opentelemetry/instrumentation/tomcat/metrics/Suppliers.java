package com.splunk.opentelemetry.instrumentation.tomcat.metrics;

import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.threads.ResizableExecutor;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import java.util.concurrent.Executor;
import java.util.function.Function;

import static java.lang.Double.NaN;

public interface Suppliers<T> {

    static Function<AbstractEndpoint<?,?>,Suppliers> fromEndpoint(AbstractEndpoint<?, ?> endpoint) {
        Executor executor = endpoint.getExecutor();
        if (executor instanceof ThreadPoolExecutor) {
            return new ThreadPoolExecutorSuppliers(endpoint);
        }
        if (executor instanceof ResizableExecutor) {
            return new ResizeableExecutorSuppliers(endpoint);
        }
        //TODO: Log warning?
        return UNHANDLED_TYPE_SUPPLIER;
    }

    Number getCurrentThreads(T executor);

    Number getActiveThreads(T executor);

    default Number getIdleThreads(T executor) {
        return getCurrentThreads(executor).intValue() - getActiveThreads(executor).intValue();
    }

    Number getCoreThreads(T executor);

    Number getMaxThreads(T executor);

    Number getSubmittedTasks(T executor);

    Number getCompletedTasks(T executor);

    Suppliers UNHANDLED_TYPE_SUPPLIER = new Suppliers<Executor>() {
        @Override
        public Number getCurrentThreads(Executor exec) {
            return NaN;
        }

        @Override
        public Number getActiveThreads(Executor exec) {
            return NaN;
        }

        @Override
        public Number getCoreThreads(Executor exec) {
            return NaN;
        }

        @Override
        public Number getMaxThreads(Executor exec) {
            return NaN;
        }

        @Override
        public Number getSubmittedTasks(Executor exec) {
            return NaN;
        }

        @Override
        public Number getCompletedTasks(Executor exec) {
            return NaN;
        }
    };
}
