package com.splunk.opentelemetry.instrumentation.tomcat.metrics;

import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.threads.ResizableExecutor;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import java.util.concurrent.Executor;
import java.util.function.Function;

import static java.lang.Double.NaN;

public interface Suppliers {

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

    Number getCurrentThreads();

    Number getActiveThreads();

    default Number getIdleThreads() {
        return getCurrentThreads().intValue() - getActiveThreads().intValue();
    }

    Number getCoreThreads();

    Number getMaxThreads();

    Number getSubmittedTasks();

    Number getCompletedTasks();

    Suppliers UNHANDLED_TYPE_SUPPLIER = new Suppliers() {
        @Override
        public Number getCurrentThreads() {
            return NaN;
        }

        @Override
        public Number getActiveThreads() {
            return NaN;
        }

        @Override
        public Number getCoreThreads() {
            return NaN;
        }

        @Override
        public Number getMaxThreads() {
            return NaN;
        }

        @Override
        public Number getSubmittedTasks() {
            return NaN;
        }

        @Override
        public Number getCompletedTasks() {
            return NaN;
        }
    };
}
