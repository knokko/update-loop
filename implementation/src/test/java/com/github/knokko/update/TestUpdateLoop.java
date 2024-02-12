package com.github.knokko.update;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assertions.*;

public class TestUpdateLoop {

    private void testConstantPeriod(long period, long sleepTime) throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        long startTime = System.nanoTime();
        UpdateLoop updater = new UpdateLoop(loop -> counter.incrementAndGet(), period);
        updater.start();

        Thread.sleep(sleepTime);
        updater.stop();
        long passedTime = System.nanoTime() - startTime;

        long expectedValue = passedTime / period;
        long actualValue = counter.get();

        if (abs(actualValue - expectedValue) > expectedValue / 5) assertEquals(expectedValue, actualValue);
    }

    @Test
    public void testLowFrequencyCounter() throws InterruptedException {
        testConstantPeriod(100_000_000, 1000);
    }

    @Test
    public void testMediumFrequencyCounter() throws InterruptedException {
        testConstantPeriod(1_000_000, 1000);
    }

    @Test
    public void testHighFrequencyCounter() throws InterruptedException {
        testConstantPeriod(2000, 1500);
    }

    @Test
    public void testWithDynamicPeriod() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        UpdateLoop updater = new UpdateLoop(loop -> counter.incrementAndGet(), 1_000_000L);

        updater.start();
        Thread.sleep(500);

        updater.setPeriod(100_000_000L);
        int midValue = counter.get();

        Thread.sleep(500);
        updater.stop();

        int finalValue = counter.get();
        int expectedValue = midValue + 5;

        if (abs(finalValue - expectedValue) > 3) assertEquals(expectedValue, finalValue);
    }

    @Test
    public void testWithDynamicMaximumBacklog() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        UpdateLoop updater = new UpdateLoop(loop -> counter.incrementAndGet(), 1_000_000L);

        updater.start();
        Thread.sleep(500);

        updater.setMaximumBacklog(1_000_000_000L);
        int midValue = counter.get();

        Thread.sleep(500);
        updater.stop();

        int finalValue = counter.get();
        int expectedValue = midValue + 500;

        if (abs(finalValue - expectedValue) > 150) assertEquals(expectedValue, finalValue);
    }

    @Test
    public void testCannotStartTwice() throws InterruptedException {
        UpdateLoop updateLoop = new UpdateLoop(loop -> {}, 100_000_000L);
        updateLoop.start();

        // Give the update thread time to start
        Thread.sleep(500);

        assertThrows(IllegalStateException.class, updateLoop::run);
        updateLoop.stop();

        assertThrows(IllegalStateException.class, updateLoop::run);
    }
}
