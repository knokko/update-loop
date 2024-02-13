package com.github.knokko.update;

import java.util.function.Consumer;

/**
 * An `UpdateLoop` tries to ensure that a given (update) function is executed periodically with a given period.
 */
public class UpdateLoop implements Runnable {

    private final Consumer<UpdateLoop> updateFunction;

    private volatile long period;
    private volatile long maximumBacklog;
    private volatile Reference reference;
    private volatile boolean shouldContinue = true;
    private volatile boolean didStart = false;

    /**
     * Constructs an <i>UpdateLoop</i> that attempts to execute {@code updateFunction} every {@code initialPeriod}
     * nanoseconds. It will allow a maximum backlog of {@code initialMaximumBacklog} nanoseconds. Any additional
     * backlog will be discarded. The period and maximum backlog can be changed at any time.
     * @param updateFunction The function that should be called periodically. The parameter will always be this
     *                       <i>UpdateLoop</i>, which is convenient for e.g. stopping it.
     * @param initialPeriod The initial period of the update function, in nanoseconds.
     * @param initialMaximumBacklog The initial maximum backlog of the update function, in nanoseconds.
     */
    public UpdateLoop(Consumer<UpdateLoop> updateFunction, long initialPeriod, long initialMaximumBacklog) {
        if (updateFunction == null || initialPeriod < 0 || initialMaximumBacklog < 0) {
            throw new IllegalArgumentException();
        }
        this.updateFunction = updateFunction;
        this.period = initialPeriod;
        this.maximumBacklog = initialMaximumBacklog;
    }

    /**
     * Constructs an <i>UpdateLoop</i> that attempts to execute {@code updateFunction} every {@code initialPeriod}
     * nanoseconds. It will use a default maximum backlog of 500 milliseconds.
     * @param updateFunction The function that should be called periodically
     * @param initialPeriod The initial period of the update function, in nanoseconds
     */
    public UpdateLoop(Consumer<UpdateLoop> updateFunction, long initialPeriod) {
        this(updateFunction, initialPeriod, 500_000_000L);
    }

    /**
     * Changes the period of this update loop to {@code newPeriod} nanoseconds. Even though this class allows any
     * non-negative period, periods below approximately 100 nanoseconds can't be achieved because the
     * update loop overhead will become larger than the update period...<br>
     * <b>Thread safety</b>: this method can be called from any thread at any time.
     * @param newPeriod The new period, in nanoseconds
     */
    public void setPeriod(long newPeriod) {
        if (newPeriod < 0) throw new IllegalArgumentException();
        reference = new Reference();
        period = newPeriod;
    }

    /**
     * Changes the maximum backlog of this update loop to {@code newBacklog} nanoseconds.<br>
     * <b>Thread safety</b>: this method can be called from any thread at any time.
     * @param newBacklog The new maximum backlog, in nanoseconds
     */
    public void setMaximumBacklog(long newBacklog) {
        if (newBacklog < 0) throw new IllegalArgumentException();
        maximumBacklog = newBacklog;
    }

    /**
     * <b>Thread safety</b>: this method can be called from any thread at any time.
     * @return The current period of this update loop, in nanoseconds.
     */
    public long getPeriod() {
        return period;
    }

    /**
     * <b>Thread safety</b>: this method can be called from any thread at any time.
     * @return The current maximum backlog of this update loop, in nanoseconds.
     */
    public long getMaximumBacklog() {
        return maximumBacklog;
    }

    /**
     * Starts this update loop on a new thread. This function must be called at most once.
     */
    public void start() {
        new Thread(this).start();
    }

    /**
     * Stops this update loop. After invoking this method, the update function will be called at most once.
     * If this method is called during the update function, the update function won't be invoked again. Calling
     * this method more than once has the same effect as calling it once.
     * <b>Thread safety</b>: this method can be called from any thread at any time.
     */
    public void stop() {
        shouldContinue = false;
    }

    private long determineSleepTime(long currentTime) {
        Reference currentReference = reference;
        long nextUpdateAt = currentReference.time + currentReference.counter * period;
        long nextSleepTime = nextUpdateAt - currentTime;

        if (-nextSleepTime > maximumBacklog) currentReference.time += -nextSleepTime - maximumBacklog;

        return nextSleepTime / 1000_000L;
    }

    /**
     * Runs this update loop on the current thread. It won't return until the update loop is finished.
     * This method must be called at most once.
     */
    @Override
    public void run() {
        if (didStart) throw new IllegalStateException("This update loop has already started");
        didStart = true;
        reference = new Reference();

        outerLoop:
        while (shouldContinue) {
            long sleepTime;
            do {
                sleepTime = determineSleepTime(System.nanoTime());
                if (sleepTime > 0L) {
                    try {
                        //noinspection BusyWait
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException interrupted) {
                        continue outerLoop;
                    }
                }
            } while (sleepTime > 0L);

            if (shouldContinue) {
                updateFunction.accept(this);
                reference.counter += 1;
            }
        }
    }
}
