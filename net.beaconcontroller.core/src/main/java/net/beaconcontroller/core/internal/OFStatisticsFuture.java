package net.beaconcontroller.core.internal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFSwitch;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.statistics.OFStatistics;

/**
 * A concrete implementation that handles asynchronously receiving OFStatistics
 * 
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFStatisticsFuture extends
        OFMessageFuture<OFStatisticsRequest, List<OFStatistics>> {

    protected volatile boolean finished;

    public OFStatisticsFuture(IBeaconProvider beaconProvider, IOFSwitch sw,
            int transactionId) {
        super(beaconProvider, sw, OFType.STATS_REPLY, transactionId);
        init();
    }

    public OFStatisticsFuture(IBeaconProvider beaconProvider, IOFSwitch sw,
            int transactionId, long timeout, TimeUnit unit) {
        super(beaconProvider, sw, OFType.STATS_REPLY, transactionId, timeout, unit);
        init();
    }

    private void init() {
        this.finished = false;
        this.result = new CopyOnWriteArrayList<OFStatistics>();
    }

    @Override
    protected void handleReply(IOFSwitch sw, OFMessage msg) {
        OFStatisticsReply sr = (OFStatisticsReply) msg;
        synchronized (this.result) {
            this.result.addAll(sr.getStatistics());
            if ((sr.getFlags() & 0x1) == 0) {
                this.finished = true;
            }
        }
    }

    @Override
    protected boolean isFinished() {
        return finished;
    }
}
