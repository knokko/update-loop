package com.github.knokko.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUpdateCounter {

    @Test
    public void testDetails() {
        UpdateCounter counter = new UpdateCounter(1000);

        // Initialize timestamp
        counter.increment(5000);

        counter.increment(5300);
        counter.increment(5600);
        counter.increment(5900);

        // First period is not yet finished
        assertEquals(-1, counter.getValue());

        counter.increment(6000);

        // There were 4 increments between 5000 (inclusive) and 6000 (exclusive)
        assertEquals(4, counter.getValue());

        for (int hundred = 1; hundred <= 10; hundred++) {
            assertEquals(4, counter.getValue());
            counter.increment(6000 + 100 * hundred);
        }

        // There were 10 increments between 6000 (inclusive) and 7000 (exclusive)
        assertEquals(10, counter.getValue());

        // Do the same test, but end at 7999 rather than 8000
        for (int hundred = 1; hundred <= 9; hundred++) {
            assertEquals(10, counter.getValue());
            counter.increment(7000 + 100 * hundred);
        }

        // There were 10 increments between 6000 (inclusive) and 7000
        counter.increment(7999);
        assertEquals(10, counter.getValue());
    }

    @Test
    public void testSimplePeriodic() {
        UpdateCounter counter = new UpdateCounter(100);

        // First round
        for (int index = 0; index < 15; index++) {
            counter.increment(10 + 10 * index);
        }
        assertEquals(10, counter.getValue());

        // Next round(s)
        for (int index = 16; index < 40; index++) {
            counter.increment(10 + 10 * index);
        }

        assertEquals(10, counter.getValue());
    }

    @Test
    public void testSimplePeriodicFaster() {
        UpdateCounter counter = new UpdateCounter(101);

        // First round
        for (int index = 0; index < 15; index++) {
            counter.increment(10 + 10 * index);
        }
        assertEquals(11, counter.getValue());

        // Next round(s)
        for (int index = 16; index < 40; index++) {
            counter.increment(10 + 10 * index);
        }

        assertEquals(11, counter.getValue());
    }

    @Test
    public void testSimplePeriodicSlower() {
        UpdateCounter counter = new UpdateCounter(99);

        // First round
        for (int index = 0; index < 15; index++) {
            counter.increment(10 + 10 * index);
        }
        assertEquals(10, counter.getValue());

        // Next round(s)
        for (int index = 16; index < 40; index++) {
            counter.increment(10 + 10 * index);
        }

        assertEquals(10, counter.getValue());
    }
}
