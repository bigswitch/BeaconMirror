package net.beaconcontroller.counter;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import net.beaconcontroller.counter.internal.ConcurrentCounter;
import net.beaconcontroller.counter.internal.CounterStore;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    private static BundleContext context;

    static BundleContext getContext() {
        return context;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
     * )
     */
    public void start(BundleContext bundleContext) throws Exception {
        Activator.context = bundleContext;
        System.out.println("started");
        try {
            CounterStore store = new CounterStore();
            final ICounter c = store.createCounter("test counter");
            Timer flushTimer = new Timer();
            flushTimer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    for (int i = 0; i < 3; i++)
                        c.increment();
                    CountSeries cs = c.snapshot(ICounter.DateSpan.SECONDS);
                    // System.out.println("counter value (secs): " + cs);
                    CountSeries cs2 = c.snapshot(ICounter.DateSpan.MINUTES);
                    // System.out.println("counter value (mins): " + cs2);

                }
            }, 1010, 1010);

        } catch (Exception e) {
            System.out.println("exception occurred...");
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception {
        Activator.context = null;
    }

}
