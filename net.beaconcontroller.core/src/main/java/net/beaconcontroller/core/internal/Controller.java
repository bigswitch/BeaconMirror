/**
 *
 */
package net.beaconcontroller.core.internal;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.IOFMessageListener.Command;

import org.openflow.example.SelectListener;
import org.openflow.example.SelectLoop;
import org.openflow.io.OFMessageAsyncStream;
import org.openflow.io.OFMessageInStream;
import org.openflow.io.OFMessageOutStream;
import org.openflow.protocol.OFEchoReply;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.protocol.factory.BasicFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Erickson (derickso@stanford.edu) - 04/04/10
 *
 */
public class Controller implements IBeaconProvider, SelectListener {
    protected static Logger logger = LoggerFactory.getLogger(Controller.class);

    protected Map<String,String> callbackOrdering;
    protected ExecutorService es;
    protected BasicFactory factory;
    protected SelectLoop listenSelectLoop;
    protected ServerSocketChannel listenSock;
    protected ConcurrentMap<OFType, List<IOFMessageListener>> messageListeners;
    protected List<IOFSwitch> switches;
    protected List<SelectLoop> switchSelectLoops;
    protected Integer threadCount;

    /**
     * 
     */
    public Controller() {
        this.messageListeners =
            new ConcurrentHashMap<OFType, List<IOFMessageListener>>();
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
        IOFSwitch sw = new OFSwitchImpl();
        OFMessageAsyncStream stream = new OFMessageAsyncStream(sock, factory);
        sw.setInputStream(stream);
        sw.setOutputStream(stream);
        sw.setSocketChannel(sock);
        switches.add(sw);
        System.err.println("New connection from " + sock);
        List<OFMessage> l = new ArrayList<OFMessage>();
        l.add(factory.getMessage(OFType.HELLO));
        l.add(factory.getMessage(OFType.FEATURES_REQUEST));
        stream.write(l);

        int ops = SelectionKey.OP_READ;
        if (stream.needsFlush())
            ops |= SelectionKey.OP_WRITE;

        // hash this switch into a thread
        SelectLoop sl = switchSelectLoops.get(sock.hashCode()
                % switchSelectLoops.size());
        sl.register(sock, ops, sw);
        // force select to return and re-enter using the new set of keys
        sl.wakeup();
    }

    protected void handleSwitchEvent(SelectionKey key, IOFSwitch sw) {
        OFMessageInStream in = sw.getInputStream();
        OFMessageOutStream out = sw.getOutputStream();
        try {
            if (key.isReadable()) {
                List<OFMessage> msgs = in.read();
                if (msgs == null) {
                    key.cancel();
                    switches.remove(sw);
                    sw.getSocketChannel().socket().close();
                    return;
                }
                handleMessages(sw, msgs);
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
            key.cancel();
            switches.remove(sw);
            try {
                sw.getSocketChannel().socket().close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
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
                    System.err.println("HELLO from " + sw);
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
                    sw.setFeaturesReply((OFFeaturesReply) m);
                    break;
                default:
                    List<IOFMessageListener> listeners = messageListeners
                            .get(m.getType());
                    if (listeners != null) {
                        for (IOFMessageListener listener : listeners) {
                            try {
                                if (Command.STOP.equals(listener.receive(sw, m))) {
                                    break;
                                }
                            } catch (Exception e) {
                                logger.error("Failure calling listener ["+
                                        listener.toString()+
                                        "] with message ["+m.toString()+
                                        "]", e);
                            }
                        }
                    } else {
                        System.err.println("Unhandled OF message: "
                                + m.getType()
                                + " from "
                                + sw.getSocketChannel().socket()
                                        .getInetAddress());
                    }
                    break;
            }
        }
    }

    public synchronized void addListener(OFType type, IOFMessageListener listener) {
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

    public synchronized void removeListener(OFType type, IOFMessageListener listener) {
        List<IOFMessageListener> listeners = messageListeners.get(type);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public void startUp() throws IOException {
        listenSock = ServerSocketChannel.open();
        listenSock.configureBlocking(false);
        listenSock.socket().bind(new java.net.InetSocketAddress(6633));
        listenSock.socket().setReuseAddress(true);

        switchSelectLoops = new ArrayList<SelectLoop>();
        switches = Collections.synchronizedList(new ArrayList<IOFSwitch>());
        threadCount = 1;
        listenSelectLoop = new SelectLoop(this);
        // register this connection for accepting
        listenSelectLoop.register(listenSock, SelectionKey.OP_ACCEPT, listenSock);

        this.factory = new BasicFactory();

        // Static number of threads equal to processor cores (+1 for listen loop)
        es = Executors.newFixedThreadPool(threadCount+1);

        // Launch one select loop per threadCount and start running
        for (int i = 0; i < threadCount; ++i) {
            final SelectLoop sl = new SelectLoop(this);
            switchSelectLoops.add(sl);
            es.execute(new Runnable() {
                public void run() {
                    try {
                        sl.doLoop();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }}
            );
        }

        es.execute(new Runnable() {
            public void run() {
                // Start the listen loop
                try {
                    listenSelectLoop.doLoop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }}
        );
        logger.info("Beacon Core Started");
    }

    public void shutDown() throws IOException {
        // shutdown listening for new switches
        listenSelectLoop.shutdown();
        listenSock.socket().close();
        listenSock.close();

        // close the switch connections
        for (Iterator<IOFSwitch> it = switches.iterator(); it.hasNext();) {
            IOFSwitch sw = it.next();
            sw.getSocketChannel().socket().close();
            it.remove();
        }

        // shutdown the connected switch select loops
        for (SelectLoop sl : switchSelectLoops) {
            sl.shutdown();
        }

        es.shutdown();
        logger.info("Beacon Core Shutdown");
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
}
