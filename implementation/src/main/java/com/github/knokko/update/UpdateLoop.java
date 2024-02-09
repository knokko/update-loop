package com.github.knokko.update;

import java.util.function.Consumer;

import static java.lang.Math.max;

/**
 * An `UpdateLoop` tries to ensure that a given (update) function is executed periodically with a given period.
 */
public class UpdateLoop implements Runnable {

    static long determineSleepTime(SlidingWindow window, long currentTime, long period) {
        SlidingEntry oldest = window.oldest();
        if (oldest == null) return 0L;

        long updateAt = oldest.value + (oldest.age + 1) * period;
        long sleepNanoTime = updateAt - currentTime;

        return sleepNanoTime / 1_000_000;
    }

    private final Consumer<UpdateLoop> updateFunction;
    private final SlidingWindow slidingWindow;

    private volatile long period;
    private volatile boolean shouldContinue = true;
    private volatile boolean didStart = false;

    /**
     * Constructs an <i>UpdateLoop</i> that attempts to execute {@code updateFunction} every {@code initialPeriod}
     * nanoseconds. It will keep track of how many updates occurred the last {@code initialPeriod * windowSize}
     * nanoseconds and use this information to maintain a stable update rate.
     * @param updateFunction The function that should be called periodically. The parameter will always be this
     *                       <i>UpdateLoop</i>, which is convenient for e.g. stopping it.
     * @param initialPeriod The initial period of the update function, in nanoseconds.
     * @param windowSize The number of past updates from which their timestamps will be remembered. This information
     *                   is needed to achieve a robust update rate.
     */
    public UpdateLoop(Consumer<UpdateLoop> updateFunction, long initialPeriod, int windowSize) {
        this.updateFunction = updateFunction;
        this.slidingWindow = new SlidingWindow(windowSize);
        this.period = initialPeriod;
    }

    /**
     * Constructs an <i>UpdateLoop</i> that attempts to execute {@code updateFunction} every {@code initialPeriod}
     * nanoseconds. It will use a default sliding window of 1 second.
     * @param updateFunction The function that should be called periodically
     * @param initialPeriod The initial period of the update function, in nanoseconds
     */
    public UpdateLoop(Consumer<UpdateLoop> updateFunction, long initialPeriod) {
        this(updateFunction, initialPeriod, max(4, (int) (1_000_000_000L / initialPeriod)));
    }

    /**
     * Changes the period of this update loop to {@code newPeriod} nanoseconds. Note that the performance of the
     * update loop may degrade if the sliding window is too small. This would for instance happen if the sliding
     * window size was determined automatically, and the new period is much smaller than the original period.<br>
     * <b>Thread safety</b>: this method can be called from any thread at any time.
     * @param newPeriod The new period, in nanoseconds
     */
    public void setPeriod(long newPeriod) {
        slidingWindow.forget();
        period = newPeriod;
    }

    /**
     * <b>Thread safety</b>: this method can be called from any thread at any time.
     * @return The current period of this update loop, in nanoseconds.
     */
    public long getPeriod() {
        return period;
    }

    /**
     * Starts this update loop on a new thread. This function must be called at most once.
     */
    public void start() {
        new Thread(this).start();
    }

    /**
     * Stops this update loop. After invoking this method, the update function will be called at most once. Calling
     * this method more than once has the same effect as calling it once.
     * <b>Thread safety</b>: this method can be called from any thread at any time.
     */
    public void stop() {
        shouldContinue = false;
    }

    /**
     * Runs this update loop on the current thread. It won't return until the update loop is finished.
     * This method must be called at most once.
     */
    @Override
    public void run() {
        if (didStart) throw new IllegalStateException("This update loop has already started");
        didStart = true;

        outerLoop:
        while (shouldContinue) {
            long sleepTime;
            do {
                sleepTime = determineSleepTime(slidingWindow, System.nanoTime(), period);
                if (sleepTime > 0L) {
                    try {
                        //noinspection BusyWait
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException interrupted) {
                        continue outerLoop;
                    }
                }
            } while (sleepTime > 0L);

            slidingWindow.insert(System.nanoTime());
            if (shouldContinue) updateFunction.accept(this);
        }
    }
}
