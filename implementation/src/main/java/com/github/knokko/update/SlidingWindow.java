package com.github.knokko.update;

class SlidingWindow {

    final long[] values;

    private int writeIndex = 0;
    private int numReadableElements = 0;

    SlidingWindow(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity (" + capacity + ") must be positive");
        this.values = new long[capacity];
    }

    synchronized void insert(long value) {
        values[writeIndex] = value;
        writeIndex = (writeIndex + 1) % values.length;
        if (numReadableElements < values.length) numReadableElements += 1;
    }

    synchronized SlidingEntry oldest() {
        if (numReadableElements == 0) return null;

        int valueIndex = (values.length + writeIndex - numReadableElements) % values.length;
        return new SlidingEntry(numReadableElements - 1, values[valueIndex]);
    }

    synchronized void forget() {
        numReadableElements = 0;
    }
}
