package com.github.knokko.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestSlidingWindow {

    @Test
    public void testSingleElementWindow() {
        SlidingWindow window = new SlidingWindow(1);
        assertNull(window.oldest());
        window.insert(5L);
        assertEquals(new SlidingEntry(0, 5L), window.oldest());
        window.insert(3L);
        assertEquals(new SlidingEntry(0, 3L), window.oldest());
        window.forget();
        assertNull(window.oldest());
        window.insert(8L);
        assertEquals(new SlidingEntry(0, 8L), window.oldest());
        window.insert(7L);
        assertEquals(new SlidingEntry(0, 7L), window.oldest());
    }

    @Test
    public void testTwoElementWindow() {
        SlidingWindow window = new SlidingWindow(2);
        assertNull(window.oldest());
        window.insert(3L);
        assertEquals(new SlidingEntry(0, 3L), window.oldest());
        window.insert(2L);
        assertEquals(new SlidingEntry(1, 3L), window.oldest());
        window.insert(5L);
        assertEquals(new SlidingEntry(1, 2L), window.oldest());

        window.forget();
        assertNull(window.oldest());
        window.insert(10L);
        assertEquals(new SlidingEntry(0, 10L), window.oldest());
        window.insert(8L);
        assertEquals(new SlidingEntry(1, 10L), window.oldest());
        window.insert(6L);
        assertEquals(new SlidingEntry(1, 8L), window.oldest());
    }

    @Test
    public void testLargerSlidingWindow() {
        SlidingWindow window = new SlidingWindow(4);
        window.insert(6L);
        window.insert(7L);
        window.insert(8L);
        assertEquals(new SlidingEntry(2, 6L), window.oldest());
        window.insert(9L);
        assertEquals(new SlidingEntry(3, 6L), window.oldest());
        window.insert(10L);
        assertEquals(new SlidingEntry(3, 7L), window.oldest());

        window.forget();
        assertNull(window.oldest());
        window.insert(15L);
        assertEquals(new SlidingEntry(0, 15L), window.oldest());
    }
}
