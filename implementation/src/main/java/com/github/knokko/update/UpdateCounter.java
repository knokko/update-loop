package com.github.knokko.update;

/**
 * A utility class for measuring the number of updates per second (or some other time interval). Usage:
 * <ul>
 *     <li>Create an instance of <i>UpdateCounter</i></li>
 *     <li>Call its <b>increment()</b> method at the start of every update</li>
 *     <li>Call its <b>getValue()</b> method to get the number of updates per period</li>
 * </ul>
 */
public class UpdateCounter {

    private final long period;

    /**
     * @param period The period of the counter. The number of updates per period will be counted.
     */
    public UpdateCounter(long period) {
        this.period = period;
    }

    /**
     * Constructs a counter with a period of 1 second.
     */
    public UpdateCounter() {
        this(1_000_000_000L);
    }

    private long referenceTime;
    private long counter = 0;
    private volatile long value = -1;

    void increment(long currentTime) {
        if (referenceTime == 0L) referenceTime = currentTime;

        long passedTime = currentTime - referenceTime;
        if (passedTime >= period) {
            value = counter;
            counter = 0;
            referenceTime = currentTime;
        }

        counter += 1;
    }

    /**
     * Increments the counter. This should be done at the start of every update (or frame).<br>
     * <b>Thread safety</b>: This method must always be called on the same thread.
     */
    public void increment() {
        increment(System.nanoTime());
    }

    /**
     * Gets the number of updates (calls to <b>increment()</b>) that happened during the last full period.
     * If no full period has completed yet, it will return -1 instead.<br>
     * <b>Thread safety</b>: This method can be called from any thread at any time.
     * Multiple threads can safely call it at the same time,
     * and even while another thread is calling <b>increment()</b>.
     */
    public long getValue() {
        return value;
    }
}
