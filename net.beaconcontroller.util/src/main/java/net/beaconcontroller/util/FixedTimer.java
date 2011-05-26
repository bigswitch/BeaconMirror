package net.beaconcontroller.util;

/**
 * Execute a task once or periodically, every period milliseconds at most.
 *
 * As long as the duration of the task is less than the timer period, the
 * task will execute every period milliseconds.  If a task iteration takes
 * longer than the period, the next iteration will begin immediately and
 * subsequent iterations will be executed every period milliseconds.
 *
 * Unlike java.util.Timer, FixedTimer does not rely on the system clock.
 * It calls System.nanoTime() which should use a monotonic clock. See
 * http://blogs.oracle.com/dholmes/entry/inside_the_hotspot_vm_clocks.
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
                        if (FixedTimer.this.period < 0)
                            break;
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

    public FixedTimer(long delay) {
        this(delay, -1);
    }

    public void cancel() {
        this.cancelled = true;
    }

    public abstract void run();
}
