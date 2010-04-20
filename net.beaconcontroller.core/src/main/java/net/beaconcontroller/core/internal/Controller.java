/**
 *
 */
package net.beaconcontroller.core.internal;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.openflow.example.SelectListener;
import org.openflow.example.SelectLoop;
import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFEchoReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.protocol.factory.BasicFactory;

/**
 * @author capveg, derickso
 *
 */
public class Controller implements IBeaconProvider, SelectListener {
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
        sw.setStream(stream);
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
        OFMessageAsyncStream stream = sw.getStream();
        try {
            if (key.isReadable()) {
                List<OFMessage> msgs = stream.read();
                if (msgs == null) {
                    key.cancel();
                    switches.remove(sw);
                    sw.getSocketChannel().socket().close();
                    return;
                }

                for (OFMessage m : msgs) {
                    switch (m.getType()) {
                        case HELLO:
                            System.err.println("HELLO from " + sw);
                            break;
                        case ECHO_REQUEST:
                            OFEchoReply reply = (OFEchoReply) stream
                                    .getMessageFactory().getMessage(
                                            OFType.ECHO_REPLY);
                            reply.setXid(m.getXid());
                            stream.write(reply);
                            break;
                        default:
                            List<IOFMessageListener> listeners = messageListeners
                                    .get(m.getType());
                            if (listeners != null) {
                                for (IOFMessageListener listener : listeners) {
                                    listener.receive(sw, m);
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
            if (key.isWritable()) {
                stream.flush();
            }

            /**
             * Only register for interest in R OR W, not both, causes stream
             * deadlock after some period of time
             */
            if (stream.needsFlush())
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

    public static void main(String [] args) throws IOException {
        CommandLine cmd = parseArgs(args);
//        int port = 6633;
//        if (cmd.hasOption("p")) {
//            port = Integer.valueOf(cmd.getOptionValue("p"));
//        }

        Controller sc = new Controller();
        if (cmd.hasOption("t"))
            sc.threadCount = Integer.valueOf(cmd.getOptionValue("t"));
        sc.startUp();
    }

    public static CommandLine parseArgs(String[] args) {
        Options options = new Options();
        options.addOption("h", "help", false, "print help");
        options.addOption("n", true, "the number of packets to send");
        options.addOption("p", "port", true, "the port to listen on, default 6633");
        options.addOption("t", "threads", true,
        "the number of threads to run");
        CommandLineParser parser = new PosixParser();
        try {
          CommandLine cmd = parser.parse(options, args);
          if (cmd.hasOption("h")) {
              printUsage(options);
              System.exit(0);
          }
          return cmd;
        } catch (ParseException e) {
          printUsage(options);
        }

        System.exit(-1);
        return null;
    }

    public static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(Controller.class.getCanonicalName(), options);
      }

    public void addListener(OFType type, IOFMessageListener listener) {
        List<IOFMessageListener> listeners = messageListeners.get(type);
        if (listeners == null) {
            // Set atomically if no list exists
            messageListeners.putIfAbsent(type,
                    new CopyOnWriteArrayList<IOFMessageListener>());
            // Get the list, the new one or any other, guaranteed not null
            listeners = messageListeners.get(type);
        }
        listeners.add(listener);
    }

    public void removeListener(OFType type, IOFMessageListener listener) {
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
    }
}
