package com.github.knokko.update;

public class UpdateCounter {

    private final long period;

    public UpdateCounter(long period) {
        this.period = period;
    }

    public UpdateCounter() {
        this(1_000_000_000L);
    }

    private long referenceTime;
    private long counter = 0;
    private volatile long value = -1;

    public void increment(long currentTime) {
        if (referenceTime == 0L) referenceTime = currentTime;

        long passedTime = currentTime - referenceTime;
        if (passedTime >= period) {
            value = counter;
            counter = 0;
            referenceTime = currentTime;
        }

        counter += 1;
    }

    public void increment() {
        increment(System.nanoTime());
    }

    public long getValue() {
        return value;
    }
}
