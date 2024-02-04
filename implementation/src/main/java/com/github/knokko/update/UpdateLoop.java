package com.github.knokko.update;

import static java.lang.Math.max;

public class UpdateLoop implements Runnable {

    static long determineSleepTime(SlidingWindow window, long currentTime, long period) {
        SlidingEntry oldest = window.oldest();
        if (oldest == null) return 0L;

        long updateAt = oldest.value + (oldest.age + 1) * period;
        long sleepNanoTime = updateAt - currentTime;

        return sleepNanoTime / 1_000_000;
    }

    private final Runnable updateFunction;
    private final SlidingWindow slidingWindow;

    private volatile long period;
    private volatile boolean shouldContinue = true;

    public UpdateLoop(Runnable updateFunction, long initialPeriod, int windowSize) {
        this.updateFunction = updateFunction;
        this.slidingWindow = new SlidingWindow(windowSize);
        this.period = initialPeriod;
    }

    public UpdateLoop(Runnable updateFunction, long initialPeriod) {
        this(updateFunction, initialPeriod, max(4, (int) (1_000_000_000L / initialPeriod)));
    }

    public void setPeriod(long newPeriod) {
        slidingWindow.forget();
        period = newPeriod;
    }

    public void start() {
        new Thread(this).start();
    }

    public void stop() {
        shouldContinue = false;
    }

    @Override
    public void run() {
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
            if (shouldContinue) updateFunction.run();
        }
    }
}
