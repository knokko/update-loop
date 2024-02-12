package com.github.knokko.update;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.awt.event.KeyEvent.*;
import static java.lang.Math.sqrt;

public class UpdateMonitor extends JFrame implements KeyListener {

    public static void main(String[] args) {

        UpdateMonitor monitor = new UpdateMonitor();
        monitor.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        monitor.setSize(1400, 800);
        monitor.setVisible(true);
        monitor.addKeyListener(monitor);

        UpdateLoop updateLoop = new UpdateLoop(monitor::updateFunction, 20_000_000L);
        updateLoop.start();

        new UpdateLoop(paintLoop -> {
            if (monitor.isDisplayable()) monitor.repaint();
            else {
                updateLoop.stop();
                paintLoop.stop();
            }
        }, 16_666_667L).start();
    }

    private final UpdateCounter updateCounter = new UpdateCounter();
    private final UpdateCounter frameCounter = new UpdateCounter();

    private final AtomicInteger startUpdateCounter = new AtomicInteger(0);
    private final AtomicInteger finishUpdateCounter = new AtomicInteger(0);

    private UpdateLoop updateLoop;

    private final int[] startCounterHistory = new int[300];
    private final int[] finishCounterHistory = new int[startCounterHistory.length];
    private final int[] updateDelayHistory = new int[startCounterHistory.length];

    private volatile int executionTime = 3;

    private volatile int spikeTime = 200;
    private volatile int spikeProbability = 2;
    private volatile boolean spaceDown;

    private final Random rng = new Random();

    private long lastStartUpdateTime = System.nanoTime();
    private final AtomicLong lastUpdateDelay = new AtomicLong(0);

    private void updateFunction(UpdateLoop updater) {
        long startUpdateTime = System.nanoTime();
        lastUpdateDelay.set(startUpdateTime - lastStartUpdateTime);
        lastStartUpdateTime = startUpdateTime;

        startUpdateCounter.incrementAndGet();
        //noinspection StatementWithEmptyBody
        while (spaceDown) ;
        updateLoop = updater;
        updateCounter.increment();
        try {
            int sleepTime = executionTime;
            if (rng.nextInt(100) < spikeProbability) sleepTime += spikeTime;
            if (sleepTime > 0) Thread.sleep(sleepTime);
        } catch (InterruptedException wakeUp) {
            // ignored
        }
        finishUpdateCounter.incrementAndGet();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        frameCounter.increment();

        int width = getWidth();
        int height = getHeight();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        int length = startCounterHistory.length;

        int startValue = startUpdateCounter.getAndSet(0);
        int finishValue = finishUpdateCounter.getAndSet(0);
        System.arraycopy(startCounterHistory, 0, startCounterHistory, 1, length - 1);
        System.arraycopy(finishCounterHistory, 0, finishCounterHistory, 1, length - 1);
        System.arraycopy(updateDelayHistory, 0, updateDelayHistory, 1, length - 1);
        startCounterHistory[0] = startValue;
        finishCounterHistory[0] = finishValue;
        updateDelayHistory[0] = (int) (5 * lastUpdateDelay.getAndSet(0) / 1000_000L);

        int baseWidth = 4;
        int baseHeight = 40;
        int offsetX = 10 + baseWidth;
        int offsetY = height - 400;

        g.setColor(new Color(0, 150, 255));
        for (int index = 0; index < length; index++) {
            int barHeight = (int) (baseHeight * sqrt(startCounterHistory[index]));
            if (barHeight > 0) {
                g.fillRect(width - offsetX - baseWidth * index, offsetY - barHeight, baseWidth, barHeight);
            }
        }

        g.setColor(new Color(150, 0, 255));
        for (int index = 0; index < length; index++) {
            int barHeight = (int) (baseHeight * sqrt(finishCounterHistory[index]));
            if (barHeight > 0) {
                g.fillRect(width - offsetX - baseWidth * index, offsetY, baseWidth, barHeight);
            }
        }

        offsetY += 350;
        g.setColor(new Color(250, 150, 0));
        for (int index = 0; index < length; index++) {
            int barHeight = updateDelayHistory[index];
            if (barHeight > 0) {
                g.fillRect(width - offsetX - baseWidth * index, offsetY - barHeight, baseWidth, barHeight);
            }
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("TimesRoman", Font.PLAIN, 20));
        g.drawString("Updates per second = " + updateCounter.getValue(), 15, 80);
        g.drawString("Frames per second = " + frameCounter.getValue(), 15, 110);
        if (updateLoop != null) {
            g.drawString("Update period = " + updateLoop.getPeriod() + "ns", 15, 140);
            @SuppressWarnings("IntegerDivisionInFloatingPointContext")
            double check = (1_000_000_000L / updateLoop.getPeriod()) / (double) updateCounter.getValue();
            g.drawString(String.format("Expected frequency / actual frequency = %.4f", check), 15, 170);
        }
        g.drawString("Execution time = " + executionTime + "ms", 15, 200);
        g.drawString("Spike time = " + spikeTime + "ms", 15, 230);
        g.drawString("Spike probability = " + spikeProbability + "%", 15, 260);
        g.drawString("Maximum backlog = " + updateLoop.getMaximumBacklog() + "ns", 15, 290);

        g.drawString("Use left/right arrow key to change the period", 600, 80);
        g.drawString("Use the a/d keys to change the execution time", 600, 110);
        g.drawString("Use shift + left/right arrow key to change the spike time", 600, 140);
        g.drawString("Use shift + a/d keys to change the spike chance", 600, 170);
        g.drawString("Hold the space bar to block the current or next update", 600, 200);
        g.drawString("Use the e/q keys to change the maximum backlog", 600, 230);
        g.drawString("The blue staves indicate how many updates started between each frame", 600, 260);
        g.drawString("The pink staves indicate how many updates finished between each frame", 600, 290);
        g.drawString("The orange staves indicate the difference between the start times of updates", 600, 320);

        Toolkit.getDefaultToolkit().sync();
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {}

    @Override
    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public void keyPressed(KeyEvent keyEvent) {
        int code = keyEvent.getKeyCode();
        long period = updateLoop.getPeriod();
        long maximumBacklog = updateLoop.getMaximumBacklog();
        if (keyEvent.isShiftDown()) {
            if (code == VK_LEFT && spikeTime > 0) spikeTime -= 10;
            if (code == VK_RIGHT) spikeTime += 10;
            if (code == VK_A && spikeProbability > 0) spikeProbability -= 1;
            if (code == VK_D && spikeProbability < 100) spikeProbability += 1;
        } else {
            if (code == VK_LEFT && period > 10) updateLoop.setPeriod(period / 2);
            if (code == VK_RIGHT && period < 100_000_000_000L) updateLoop.setPeriod(period * 2);
            if (code == VK_A && executionTime > 0) executionTime -= 1;
            if (code == VK_D) executionTime += 1;
            if (code == VK_Q && maximumBacklog > 100) updateLoop.setMaximumBacklog(maximumBacklog / 2);
            if (code == VK_E && maximumBacklog < 100_000_000_000L) updateLoop.setMaximumBacklog(maximumBacklog * 2);
        }
        if (code == VK_SPACE) spaceDown = true;
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == VK_SPACE) spaceDown = false;
    }
}
