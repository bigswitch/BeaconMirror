package net.beaconcontroller.util;

/**
 * Execute a task periodically, every period milliseconds at most.
 *
 * As long as the duration of the task is less than the timer period, the
 * task will execute every period milliseconds.  If a task iteration takes
 * longer than the period, the next iteration will begin immediately and
 * subsequent iterations will be executed every period milliseconds.
 *
 * Unlike java.util.Timer, FixedTimer does not rely on the system clock.
 */
public abstract class FixedTimer {

    Thread thread;
    long delay;
    long period;
    boolean cancelled;

    public static long now() {
        return System.nanoTime() / 1000 / 1000;
    }

    public FixedTimer(long delay, long period) {
        this.delay = delay;
        this.period = period;
        this.cancelled = false;

        this.thread = new Thread() {
            long nextRun;
            public void run() {
                try {
                    sleep(FixedTimer.this.delay);
                    nextRun = FixedTimer.now();
                    while (!FixedTimer.this.cancelled) {
                        FixedTimer.this.run();
                        long now = FixedTimer.now();
                        nextRun += FixedTimer.this.period;
                        if (nextRun > now)
                            sleep(nextRun - now);
                        else
                            nextRun = now;
                    }
                }
                catch (InterruptedException e) { }
            }
        };
        this.thread.start();
    }

    public void cancel() {
        this.cancelled = true;
    }

    public abstract void run();
}
