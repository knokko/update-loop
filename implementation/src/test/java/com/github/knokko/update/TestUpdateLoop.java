package com.github.knokko.update;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static com.github.knokko.update.UpdateLoop.determineSleepTime;
import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUpdateLoop {

    @Test
    public void testDetermineSleepTime() {
        assertEquals(0L, determineSleepTime(new SlidingWindow(5), 1234, 10));

        long m = 1_000_000L;
        long period = 10 * m;
        SlidingWindow window = new SlidingWindow(4);

        // Oldest entry is (0, 2500)
        window.insert(2500 * m);
        assertEquals(7, determineSleepTime(window, 2503 * m, period));
        assertEquals(6, determineSleepTime(window, 1 + 2503 * m, period));

        // Oldest entry is (1, 2500)
        window.insert(2510 * m);
        assertEquals(17, determineSleepTime(window, 2503 * m, period));
        assertEquals(7, determineSleepTime(window, 2513 * m, period));
        assertTrue(determineSleepTime(window, 2523 * m, period) <= 0);

        // Oldest entry is (2, 2500)
        window.insert(2600 * m);
        assertEquals(7, determineSleepTime(window, 2523 * m, period));
        assertTrue(determineSleepTime(window, 2550 * m, period) <= 0);

        // Oldest entry is (3, 2500)
        window.insert(2601 * m);
        assertEquals(37, determineSleepTime(window, 2503 * m, period));
        assertTrue(determineSleepTime(window, 2540 * m, period) <= 0);

        // Oldest entry becomes 2510
        window.insert(2602 * m);
        assertEquals(4L, determineSleepTime(window, 2546 * m, period));
        assertTrue(determineSleepTime(window, 1 + 2549 * m, period) <= 0);
        assertTrue(determineSleepTime(window, 2615 * m, period) <= 0);

        // Oldest entry becomes 2600
        window.insert(2610 * m);
        assertEquals(8L, determineSleepTime(window, 2632 * m, period));
    }

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
        testConstantPeriod(500, 1500);
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
}
