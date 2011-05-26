package net.beaconcontroller.core.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.IOFSwitchFilter;
import net.beaconcontroller.core.IOFSwitchListener;
import net.beaconcontroller.util.FixedTimer;

/**
 * A Future object used to retrieve asynchronous OFMessage replies. Unregisters
 * and cancels itself by default after 60 seconds. This class is meant to be
 * sub-classed and proper behavior added to the handleReply method, and
 * termination of the Future to be handled in the isFinished method.
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public abstract class OFMessageFuture<T,V> implements Future<V>, IOFMessageListener,
        IOFSwitchFilter, IOFSwitchListener {

    protected IBeaconProvider beaconProvider;
    protected volatile boolean canceled;
    protected CountDownLatch latch;
    protected OFType responseType;
    protected volatile V result;
    protected IOFSwitch sw;
    protected FixedTimer timeoutTimer;
    protected int transactionId;

    public OFMessageFuture(IBeaconProvider beaconProvider, IOFSwitch sw,
            OFType responseType, int transactionId) {
        this(beaconProvider, sw, responseType, transactionId, 60, TimeUnit.SECONDS);
    }

    public OFMessageFuture(IBeaconProvider beaconProvider, IOFSwitch sw,
            OFType responseType, int transactionId, long timeout, TimeUnit unit) {
        this.beaconProvider = beaconProvider;
        this.canceled = false;
        this.latch = new CountDownLatch(1);
        this.responseType = responseType;
        this.sw = sw;
        this.transactionId = transactionId;

        final OFMessageFuture<T, V> future = this;
        this.timeoutTimer = new FixedTimer(unit.toMillis(timeout)) {
            @Override
            public void run() {
                future.cancel(true);
            }};
    }

    protected void unRegister() {
        this.beaconProvider.removeOFMessageListener(this.responseType, this);
        this.beaconProvider.removeOFSwitchListener(this);
    }

    @Override
    public void addedSwitch(IOFSwitch sw) {
        // Noop
    }

    @Override
    public void removedSwitch(IOFSwitch sw) {
        if (this.sw.equals(sw)) {
            unRegister();
            this.latch.countDown();
        }
    }

    @Override
    public boolean isInterested(IOFSwitch sw) {
        if (this.sw.equals(sw)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg) {
        if (transactionId == msg.getXid()) {
            handleReply(sw, msg);
            if (isFinished()) {
                unRegister();
                this.timeoutTimer.cancel();
                this.latch.countDown();
            }
            return Command.STOP;
        } else {
            return Command.CONTINUE;
        }
    }

    /**
     * Used to handle the specific expected message this Future was reigstered
     * for, the specified msg parameter is guaranteed to match the type and
     * transaction id specified.
     * @param sw
     * @param msg
     * @return
     */
    protected abstract void handleReply(IOFSwitch sw, OFMessage msg);

    /**
     * Called directly after handleReply, subclasses implement this method to
     * indicate when the future can deregister itself from receiving future
     * messages, and when it is safe to return the results to any waiting
     * threads.
     * @return when this Future has completed its work
     */
    protected abstract boolean isFinished();

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /* (non-Javadoc)
     * @see java.util.concurrent.Future#cancel(boolean)
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone()) {
            return false;
        } else {
            unRegister();
            canceled = true;
            this.latch.countDown();
            return !isDone();
        }
    }

    /* (non-Javadoc)
     * @see java.util.concurrent.Future#isCancelled()
     */
    @Override
    public boolean isCancelled() {
        return canceled;
    }

    /* (non-Javadoc)
     * @see java.util.concurrent.Future#isDone()
     */
    @Override
    public boolean isDone() {
        return this.latch.getCount() == 0;
    }

    /* (non-Javadoc)
     * @see java.util.concurrent.Future#get()
     */
    @Override
    public V get() throws InterruptedException, ExecutionException {
        this.latch.await();
        return result;
    }

    /* (non-Javadoc)
     * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        this.latch.await(timeout, unit);
        return result;
    }

}
