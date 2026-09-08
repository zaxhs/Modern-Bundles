package dev.modernbundles.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BundleScrollAccumulatorTest {
    @Test
    void resetDiscardsFractionalScrollInput() {
        BundleScrollAccumulator accumulator = new BundleScrollAccumulator();

        assertEquals(new BundleScrollAccumulator.Step(0, 0), accumulator.add(0.0, 0.75));
        accumulator.reset();
        assertEquals(new BundleScrollAccumulator.Step(0, 0), accumulator.add(0.0, 0.5));
        assertEquals(new BundleScrollAccumulator.Step(0, 1), accumulator.add(0.0, 0.5));
    }
}

