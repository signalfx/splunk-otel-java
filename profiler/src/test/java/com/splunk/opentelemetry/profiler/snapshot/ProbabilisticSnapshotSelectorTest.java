package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProbabilisticSnapshotSelectorTest {
    private static final int ITERATIONS = 100;
    private static final int TRACES_PER_ITERATIONS = 1000;
    private static final double SELECTION_RATE = 0.05;
    private final static List<Integer> OUTCOMES = new ArrayList<>();

    private final ProbabilisticSnapshotSelector selector = new ProbabilisticSnapshotSelector(SELECTION_RATE);

    @RepeatedTest(ITERATIONS)
    @Order(1)
    void processTraces() {
        int selected = 0;
        for (int i = 0; i < TRACES_PER_ITERATIONS; i++) {
            if (selector.select()) {
                selected++;
            }
        }
        OUTCOMES.add(selected);
    }

    @Test
    @Order(2)
    void evaluatePercentAllowed() {
        assertEquals(SELECTION_RATE * 100, percentAllowed(), 1.0);
    }

    private double percentAllowed() {
        int allowed = OUTCOMES.stream().mapToInt(Integer::intValue).sum();
        int considered = TRACES_PER_ITERATIONS * OUTCOMES.size();
        return ((double) allowed / (double) (considered)) * 100;
    }

    @Test
    @Order(3)
    void evaluateOutliers() {
        assertThat(outliers()).isLessThan((int) (TRACES_PER_ITERATIONS * SELECTION_RATE));
    }

    private long outliers() {
        double expectedSelectionsPerIteration = TRACES_PER_ITERATIONS * SELECTION_RATE;
        int upper = (int)(expectedSelectionsPerIteration * 1.1);
        int lower = (int)(expectedSelectionsPerIteration * .9);
        return OUTCOMES.stream().filter(i -> i < lower || i > upper).count();
    }
}
