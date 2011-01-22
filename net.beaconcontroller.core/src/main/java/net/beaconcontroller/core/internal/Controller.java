/**
 *
 */
package net.beaconcontroller.core.internal;

import java.io.IOException;
import java.io.EOFException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import net.beaconcontroller.core.dao.IControllerDao;
import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFController;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFMessageListener.Command;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.IOFSwitchFilter;
import net.beaconcontroller.core.IOFSwitchListener;
import net.beaconcontroller.core.io.internal.OFStream;
import net.beaconcontroller.packet.IPv4;

import org.openflow.example.SelectListener;
import org.openflow.example.SelectLoop;
import org.openflow.io.OFMessageInStream;
import org.openflow.io.OFMessageOutStream;
import org.openflow.protocol.OFEchoReply;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFError.OFErrorType;
import org.openflow.protocol.OFError.OFFlowModFailedCode;
import org.openflow.protocol.OFError.OFHelloFailedCode;
import org.openflow.protocol.OFError.OFPortModFailedCode;
import org.openflow.protocol.OFError.OFQueueOpFailedCode;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFGetConfigReply;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFSetConfig;
import org.openflow.protocol.OFType;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.util.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - 04/04/10
 *
 */
public class Controller implements IBeaconProvider, IOFController, SelectListener {
    protected static Logger log = LoggerFactory.getLogger(Controller.class);
    protected static String SWITCH_REQUIREMENTS_TIMER_KEY = "SW_REQ_TIMER";

    protected Map<String,String> callbackOrdering;
    protected ExecutorService es;
    protected BasicFactory factory;
    protected String listenAddress;
    protected int listenPort = 6633;
    protected SelectLoop listenSelectLoop;
    protected ServerSocketChannel listenSock;
    protected ConcurrentMap<OFType, List<IOFMessageListener>> messageListeners;
    protected volatile boolean shuttingDown = false;
    protected ConcurrentHashMap<Long, IOFSwitch> switches;
    protected Set<IOFSwitchListener> switchListeners;
    protected List<SelectLoop> switchSelectLoops;
    protected Integer threadCount;
    protected BlockingQueue<Update> updates;
    protected Thread updatesThread;

    protected class Update {
        public IOFSwitch sw;
        public boolean added;

        public Update(IOFSwitch sw, boolean added) {
            this.sw = sw;
            this.added = added;
        }
    }

    private IControllerDao coreDao;
    
    /**
     * 
     */
    public Controller() {
        this.messageListeners =
            new ConcurrentHashMap<OFType, List<IOFMessageListener>>();
        this.switchListeners = new CopyOnWriteArraySet<IOFSwitchListener>();
        this.updates = new LinkedBlockingQueue<Update>();
    }

    public void handleEvent(SelectionKey key, Object arg) throws IOException {
        if (arg instanceof ServerSocketChannel)
            handleListenEvent(key, (ServerSocketChannel)arg);
        else
            handleSwitchEvent(key, (IOFSwitch) arg);
    }

    protected void handleListenEvent(SelectionKey key, ServerSocketChannel ssc)
            throws IOException {
        SocketChannel sock = listenSock.accept();
        log.info("Switch connected from {}", sock.toString());
        sock.socket().setTcpNoDelay(true);
        sock.configureBlocking(false);
        OFSwitchImpl sw = new OFSwitchImpl();
        // hash this switch into a thread
        final SelectLoop sl = switchSelectLoops.get(sock.hashCode()
                % switchSelectLoops.size());

        // register initially with no ops because we need the key to init the stream
        SelectionKey switchKey = sl.registerBlocking(sock, 0, sw);
        OFStream stream = new OFStream(sock, factory, switchKey);
        sw.setInputStream(stream);
        sw.setOutputStream(stream);
        sw.setSocketChannel(sock);
        sw.setBeaconProvider(this);

        // register for read
        switchKey.interestOps(SelectionKey.OP_READ);
        sl.wakeup();

        // Send HELLO
        stream.write(factory.getMessage(OFType.HELLO));
    }

    protected void handleSwitchEvent(SelectionKey key, IOFSwitch sw) {
        OFMessageInStream in = sw.getInputStream();
        OFMessageOutStream out = sw.getOutputStream();
        try {
            /**
             * A key may not be valid here if it has been disconnected while
             * it was in a select operation.
             */
            if (!key.isValid())
                return;

            if (key.isReadable()) {
                List<OFMessage> msgs = in.read();
                if (msgs == null) {
                    // if the other end closed its end of the connection, flush
                    // any remaining written data before closing our end
                    // (otherwise the socket hangs around forever in CLOSE_WAIT
                    // state, and the associated stream and buffer objects are
                    // never freed)
                    if (!out.needsFlush())
                        throw new EOFException();
                } else {
                    handleMessages(sw, msgs);
                }
            }

            if (key.isWritable()) {
                out.flush();
            }

            /**
             * Only register for interest in R OR W, not both, causes stream
             * deadlock after some period of time
             */
            if (out.needsFlush())
                key.interestOps(SelectionKey.OP_WRITE);
            else
                key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            // if we have an exception, disconnect the switch
            disconnectSwitch(key, sw);
        }
    }

    /**
     * Disconnect the switch from Beacon
     */
    protected void disconnectSwitch(SelectionKey key, IOFSwitch sw) {
        key.cancel();
        stopSwitchRequirementsTimer(sw);
        // only remove if we have a features reply (DPID)
        if (sw.getFeaturesReply() != null)
            removeSwitch(sw);
        try {
            sw.getSocketChannel().socket().close();
        } catch (IOException e1) {
        }
        log.info("Switch disconnected {}", sw);
    }

    /**
     * Handle replies to certain OFMessages, and pass others off to listeners
     * @param sw
     * @param msgs
     * @throws IOException
     */
    protected void handleMessages(IOFSwitch sw, List<OFMessage> msgs)
            throws IOException {
        for (OFMessage m : msgs) {
            switch (m.getType()) {
                case HELLO:
                    log.debug("HELLO from {}", sw);
                    // Send initial Features Request
                    sw.getOutputStream().write(factory.getMessage(OFType.FEATURES_REQUEST));

                    // Delete all pre-existing flows
                    OFMatch match = new OFMatch().setWildcards(OFMatch.OFPFW_ALL);
                    OFMessage fm = ((OFFlowMod) sw.getInputStream().getMessageFactory()
                        .getMessage(OFType.FLOW_MOD))
                        .setMatch(match)
                        .setCommand(OFFlowMod.OFPFC_DELETE)
                        .setOutPort(OFPort.OFPP_NONE)
                        .setLength(U16.t(OFFlowMod.MINIMUM_LENGTH));
                    sw.getOutputStream().write(fm);

                    // Start required message timer
                    startSwitchRequirementsTimer(sw);
                    break;
                case ECHO_REQUEST:
                    OFMessageInStream in = sw.getInputStream();
                    OFMessageOutStream out = sw.getOutputStream();
                    OFEchoReply reply = (OFEchoReply) in
                            .getMessageFactory().getMessage(
                                    OFType.ECHO_REPLY);
                    reply.setXid(m.getXid());
                    out.write(reply);
                    break;
                case FEATURES_REPLY:
                    log.debug("Features Reply from {}", sw);
                    sw.setFeaturesReply((OFFeaturesReply) m);
                    addSwitch(sw);
                    break;
                case GET_CONFIG_REPLY:
                    OFGetConfigReply cr = (OFGetConfigReply) m;
                    if (cr.getMissSendLength() == (short)0xffff) {
                        log.debug("Config Reply from {} confirms miss length set to 0xffff", sw);
                        stopSwitchRequirementsTimer(sw);
                    }
                    break;
                case ERROR:
                    OFError error = (OFError) m;
                    logError(sw, error);
                    break;
                default:
                    // Don't pass along messages until we have the features reply
                    if (sw.getFeaturesReply() == null) {
                        log.warn("Message type {} received from switch " +
                            "{} before receiving a features reply.", m.getType(), sw);
                        break;
                    }
                    List<IOFMessageListener> listeners = messageListeners
                            .get(m.getType());
                    if (listeners != null) {
                        for (IOFMessageListener listener : listeners) {
                            try {
                                if (listener instanceof IOFSwitchFilter) {
                                    if (!((IOFSwitchFilter)listener).isInterested(sw)) {
                                        break;
                                    }
                                }
                                if (Command.STOP.equals(listener.receive(sw, m))) {
                                    break;
                                }
                            } catch (Exception e) {
                                log.error("Failure calling listener ["+
                                        listener.toString()+
                                        "] with message ["+m.toString()+
                                        "]", e);
                            }
                        }
                    } else {
                        log.error("Unhandled OF Message: {} from {}", m, sw);
                    }
                    break;
            }
        }
    }

    protected void logError(IOFSwitch sw, OFError error) {
        // TODO Move this to OFJ with *much* better printing
        OFErrorType et = OFErrorType.values()[0xffff & error.getErrorType()];
        switch (et) {
            case OFPET_HELLO_FAILED:
                OFHelloFailedCode hfc = OFHelloFailedCode.values()[0xffff & error.getErrorCode()];
                log.error("Error {} {} from {}", new Object[] {et, hfc, sw});
                break;
            case OFPET_BAD_REQUEST:
                OFBadRequestCode brc = OFBadRequestCode.values()[0xffff & error.getErrorCode()];
                log.error("Error {} {} from {}", new Object[] {et, brc, sw});
                break;
            case OFPET_BAD_ACTION:
                OFBadActionCode bac = OFBadActionCode.values()[0xffff & error.getErrorCode()];
                log.error("Error {} {} from {}", new Object[] {et, bac, sw});
                break;
            case OFPET_FLOW_MOD_FAILED:
                OFFlowModFailedCode fmfc = OFFlowModFailedCode.values()[0xffff & error.getErrorCode()];
                log.error("Error {} {} from {}", new Object[] {et, fmfc, sw});
                break;
            case OFPET_PORT_MOD_FAILED:
                OFPortModFailedCode pmfc = OFPortModFailedCode.values()[0xffff & error.getErrorCode()];
                log.error("Error {} {} from {}", new Object[] {et, pmfc, sw});
                break;
            case OFPET_QUEUE_OP_FAILED:
                OFQueueOpFailedCode qofc = OFQueueOpFailedCode.values()[0xffff & error.getErrorCode()];
                log.error("Error {} {} from {}", new Object[] {et, qofc, sw});
                break;
            default:
                break;
        }
    }

    /**
     * Creates a timer that keeps requesting a switch's feature reply until it
     * is received, then continues sending set config/get config messages until
     * the timer is canceled.
     * @param sw
     */
    protected void startSwitchRequirementsTimer(final IOFSwitch sw) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (sw.getSocketChannel().isConnected()) {
                        if (sw.getFeaturesReply() == null) {
                            // send another features request
                            sw.getOutputStream().write(factory.getMessage(OFType.FEATURES_REQUEST));
                        } else {
                            // Ensure we receive the full packet via PacketIn
                            OFSetConfig config = (OFSetConfig) factory
                                    .getMessage(OFType.SET_CONFIG);
                            config.setMissSendLength((short) 0xffff)
                                    .setLengthU(OFSetConfig.MINIMUM_LENGTH);
                            sw.getOutputStream().write(config);
                            sw.getOutputStream().write(factory.getMessage(OFType.GET_CONFIG_REQUEST));
                        }
                    } else {
                        // stop timer
                        stopSwitchRequirementsTimer(sw);
                    }
                } catch (Exception e) {
                    stopSwitchRequirementsTimer(sw);
                    log.error("Exception in switch requirements timer", e);
                }
            }}, 500, 500);
        sw.getAttributes().put(SWITCH_REQUIREMENTS_TIMER_KEY, timer);
    }

    protected void stopSwitchRequirementsTimer(final IOFSwitch sw) {
        Timer timer = (Timer) sw.getAttributes().get(
                SWITCH_REQUIREMENTS_TIMER_KEY);
        if (timer != null) {
            timer.cancel();
            sw.getAttributes().remove(SWITCH_REQUIREMENTS_TIMER_KEY);
        }
    }

    public synchronized void addOFMessageListener(OFType type, IOFMessageListener listener) {
        List<IOFMessageListener> listeners = messageListeners.get(type);
        if (listeners == null) {
            // Set atomically if no list exists
            messageListeners.putIfAbsent(type,
                    new CopyOnWriteArrayList<IOFMessageListener>());
            // Get the list, the new one or any other, guaranteed not null
            listeners = messageListeners.get(type);
        }

        if (callbackOrdering != null &&
                callbackOrdering.containsKey(type.toString()) &&
                callbackOrdering.get(type.toString()).contains(listener.getName())) {
            String order = callbackOrdering.get(type.toString());
            String[] orderArray = order.split(",");
            int myPos = 0;
            for (int i = 0; i < orderArray.length; ++i) {
                orderArray[i] = orderArray[i].trim();
                if (orderArray[i].equals(listener.getName()))
                    myPos = i;
            }
            List<String> beforeList = Arrays.asList(Arrays.copyOfRange(orderArray, 0, myPos));

            boolean added = false;
            // only try and walk if there are already listeners
            if (listeners.size() > 0) {
                // Walk through and determine where to insert
                for (int i = 0; i < listeners.size(); ++i) {
                    if (beforeList.contains(listeners.get(i).getName()))
                        continue;
                    listeners.add(i, listener);
                    added = true;
                    break;
                }
            }
            if (!added) {
                listeners.add(listener);
            }

        } else {
            listeners.add(listener);
        }
    }

    public synchronized void removeOFMessageListener(OFType type, IOFMessageListener listener) {
        List<IOFMessageListener> listeners = messageListeners.get(type);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public void startUp() throws IOException {
        listenSock = ServerSocketChannel.open();
        listenSock.configureBlocking(false);
        if (listenAddress != null) {
            listenSock.socket().bind(
                    new java.net.InetSocketAddress(InetAddress
                            .getByAddress(IPv4
                                    .toIPv4AddressBytes(listenAddress)),
                            listenPort));
        } else {
            listenSock.socket().bind(new java.net.InetSocketAddress(listenPort));
        }
        listenSock.socket().setReuseAddress(true);
        log.info("Controller listening on {}:{}", listenAddress == null ? "*"
                : listenAddress, listenPort);

        switchSelectLoops = new ArrayList<SelectLoop>();
        switches = new ConcurrentHashMap<Long, IOFSwitch>();

        if (threadCount == null)
            threadCount = 1;

        listenSelectLoop = new SelectLoop(this);
        // register this connection for accepting
        listenSelectLoop.register(listenSock, SelectionKey.OP_ACCEPT, listenSock);

        this.factory = new BasicFactory();

        // This call is before starting up the threads because the DAO resets the
        // persistent switch state to clean up after a possible abnormal termination
        // of the beacon process. If this happened after the listen and switch select
        // loops were active then there could be a potential race condition between
        // deactivating the switches and a switch connecting to beacon.
        if (coreDao != null) {
            try {
                coreDao.startedController(this);
            }
            catch (Exception e) {
                log.error("Error writing controller startup info to database", e);
            }
        }
            
        // Static number of threads equal to processor cores (+1 for listen loop)
        es = Executors.newFixedThreadPool(threadCount+1);

        // Launch one select loop per threadCount and start running
        for (int i = 0; i < threadCount; ++i) {
            final SelectLoop sl = new SelectLoop(this, 500);
            switchSelectLoops.add(sl);
            es.execute(new Runnable() {
                public void run() {
                    try {
                        sl.doLoop();
                    } catch (Exception e) {
                        log.error("Exception during worker loop, terminating thread", e);
                    }
                }}
            );
        }

        es.execute(new Runnable() {
            public void run() {
                // Start the listen loop
                try {
                    listenSelectLoop.doLoop();
                } catch (Exception e) {
                    log.error("Exception during accept loop, terminating thread", e);
                }
            }}
        );

        updatesThread = new Thread(new Runnable () {
            @Override
            public void run() {
                while (true) {
                    try {
                        Update update = updates.take();
                        if (coreDao != null) {
                            try {
                                if (update.added)
                                    coreDao.addedSwitch(update.sw);
                                else
                                    coreDao.removedSwitch(update.sw);
                            }
                            catch (Exception e) {
                                log.error("Error updating switch info in database", e);
                            }
                        }
                        if (switchListeners != null) {
                            for (IOFSwitchListener listener : switchListeners) {
                                try {
                                    if (update.added)
                                        listener.addedSwitch(update.sw);
                                    else
                                        listener.removedSwitch(update.sw);
                                } catch (Exception e) {
                                    log.error("Error calling switch listener", e);
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        log.warn("Controller updates thread interupted", e);
                        if (shuttingDown)
                            return;
                    }
                }
            }}, "Controller Updates");
        updatesThread.start();
        
        log.info("Beacon Core Started");
    }

    public void shutDown() throws IOException {
        shuttingDown = true;
        // shutdown listening for new switches
        listenSelectLoop.shutdown();
        listenSock.socket().close();
        listenSock.close();

        // close the switch connections
        for (Iterator<Entry<Long, IOFSwitch>> it = switches.entrySet().iterator(); it.hasNext();) {
            Entry<Long, IOFSwitch> entry = it.next();
            entry.getValue().getSocketChannel().socket().close();
            it.remove();
        }

        // shutdown the connected switch select loops
        for (SelectLoop sl : switchSelectLoops) {
            sl.shutdown();
        }

        es.shutdown();
        updatesThread.interrupt();

        if (coreDao != null) {
            try {
                coreDao.shutDownController(this);
            }
            catch (Exception e) {
                log.error("Error writing controller shutdown info to database", e);
            }
        }
        
        log.info("Beacon Core Shutdown");
    }

    /**
     * @param callbackOrdering the callbackOrdering to set
     */
    public void setCallbackOrdering(Map<String, String> callbackOrdering) {
        this.callbackOrdering = callbackOrdering;
    }

    /**
     * @return the messageListeners
     */
    protected ConcurrentMap<OFType, List<IOFMessageListener>> getMessageListeners() {
        return messageListeners;
    }

    /**
     * @param messageListeners the messageListeners to set
     */
    protected void setMessageListeners(
            ConcurrentMap<OFType, List<IOFMessageListener>> messageListeners) {
        this.messageListeners = messageListeners;
    }

    @Override
    public Map<Long, IOFSwitch> getSwitches() {
        return this.switches;
    }

    @Override
    public void addOFSwitchListener(IOFSwitchListener listener) {
        this.switchListeners.add(listener);
    }

    @Override
    public void removeOFSwitchListener(IOFSwitchListener listener) {
        this.switchListeners.remove(listener);
    }

    /**
     * Adds a switch that has connected and returned a features reply, then
     * calls all related listeners
     * @param sw the new switch
     */
    protected void addSwitch(IOFSwitch sw) {
        this.switches.put(sw.getId(), sw);
        if (coreDao != null) {
            try {
                coreDao.addedSwitch(sw);
            }
            catch (Exception e) {
                log.error("Error writing added switch info to database", e);
            }
        }
        Update update = new Update(sw, true);
        try {
            this.updates.put(update);
        } catch (InterruptedException e) {
            log.error("Failure adding update to queue", e);
        }
    }

    /**
     * Removes a disconnected switch and calls all related listeners
     * @param sw the switch that has disconnected
     */
    protected void removeSwitch(IOFSwitch sw) {
        if (!this.switches.remove(sw.getId(), sw)) {
            log.warn("Removing switch {} has already been replaced", sw);
        }
        if (coreDao != null) {
            try {
                coreDao.removedSwitch(sw);
            }
            catch (Exception e) {
                log.error("Error writing removed switch info to database", e);
            }
        }
        Update update = new Update(sw, false);
        try {
            this.updates.put(update);
        } catch (InterruptedException e) {
            log.error("Failure adding update to queue", e);
        }
    }

    @Override
    public Map<OFType, List<IOFMessageListener>> getListeners() {
        return Collections.unmodifiableMap(this.messageListeners);
    }

    @Override
    public String getControllerId() {
        return getListenAddress() + ":" + Integer.toString(getListenPort());
    }
    
    @Override
    public String getListenAddress() {
        if (listenAddress != null)
            return listenAddress;
        
        String localAddress = "UnknownAddress";
        try {
            localAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (Exception e) {
            log.error("Error getting local IP address", e);
        }
        return localAddress;
    }
    
    @Override
    public int getListenPort() {
        return listenPort;
    }

    public void setControllerDao(IControllerDao coreDao) {
        this.coreDao = coreDao;
    }
    
    /**
     * @param listenAddress the listenAddress to set
     */
    public void setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
    }

    /**
     * @param listenPort the listenPort to set
     */
    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    /**
     * @param threadCount the threadCount to set
     */
    public void setThreadCount(Integer threadCount) {
        this.threadCount = threadCount;
    }
}
