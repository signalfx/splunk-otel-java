package com.splunk.opentelemetry.profiler;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;

/**
 * Abstraction around the Java Flight Recorder subsystem.
 */
public class JFR {

    public final static JFR instance = new JFR();

    public boolean isAvailable(){
        return FlightRecorder.isAvailable();
    }

    public Recording takeSnapshot(){
        return FlightRecorder.getFlightRecorder().takeSnapshot();
    }

}
