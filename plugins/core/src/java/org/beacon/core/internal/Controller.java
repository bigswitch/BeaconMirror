/**
 *
 */
package org.beacon.core.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.beacon.core.IBeaconProvider;
import org.beacon.core.IOFMessageListener;
import org.openflow.example.SelectListener;
import org.openflow.example.SelectLoop;
import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.util.LRULinkedHashMap;
import org.openflow.util.U16;

/**
 * @author capveg, derickso
 *
 */
public class Controller implements IBeaconProvider, SelectListener {
    protected ExecutorService es;
    protected BasicFactory factory;
    protected SelectLoop listenSelectLoop;
    protected ServerSocketChannel listenSock;
    protected List<SelectLoop> switchSelectLoops;
    protected Map<SocketChannel,OFSwitch> switchSockets;
    protected Integer threadCount;

    protected class OFSwitch {
        protected SocketChannel sock;
        protected OFMessageAsyncStream stream;
        protected Map<Integer, Short> macTable =
            new LRULinkedHashMap<Integer, Short>(64001, 64000);

        public OFSwitch(SocketChannel sock, OFMessageAsyncStream stream) {
            this.sock = sock;
            this.stream = stream;
        }

        public void handlePacketIn(OFPacketIn pi) {
            // Build the Match
            OFMatch match = new OFMatch();
            match.loadFromPacket(pi.getPacketData());
            byte[] dlDst = match.getDataLayerDestination();
            Integer dlDstKey = Arrays.hashCode(dlDst);
            byte[] dlSrc = match.getDataLayerSource();
            Integer dlSrcKey = Arrays.hashCode(dlSrc);
            int bufferId = pi.getBufferId();

            // if the src is not multicast, learn it
            if ((dlSrc[0] & 0x1) == 0) {
                if (!macTable.containsKey(dlSrcKey) ||
                        !macTable.get(dlSrcKey).equals(pi.getInPort())) {
                    macTable.put(dlSrcKey, pi.getInPort());
                }
            }

            Short outPort = null;
            // if the destination is not multicast, look it up
            if ((dlDst[0] & 0x1) == 0) {
                outPort = macTable.get(dlDstKey);
            }

            // push a flow mod if we know where the packet should be going
            if (outPort != null) {
                OFFlowMod fm = (OFFlowMod) factory.getMessage(OFType.FLOW_MOD);
                fm.setBufferId(bufferId);
                fm.setCommand((short) 0);
                fm.setCookie(0);
                fm.setFlags((short) 0);
                fm.setHardTimeout((short) 0);
                fm.setIdleTimeout((short) 5);
                match.setInputPort(pi.getInPort());
                match.setWildcards(0);
                fm.setMatch(match);
                fm.setOutPort((short) OFPort.OFPP_NONE.getValue());
                fm.setPriority((short) 0);
                OFActionOutput action = new OFActionOutput();
                action.setMaxLength((short) 0);
                action.setPort(outPort);
                List<OFAction> actions = new ArrayList<OFAction>();
                actions.add(action);
                fm.setActions(actions);
                fm.setLength(U16.t(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH));
                stream.write(fm);
            }

            // Send a packet out
            if (outPort == null || pi.getBufferId() == 0xffffffff) {
               OFPacketOut po = new OFPacketOut();
               po.setBufferId(bufferId);
               po.setInPort(pi.getInPort());

               // set actions
               OFActionOutput action = new OFActionOutput();
               action.setMaxLength((short) 0);
               action.setPort((short) ((outPort == null) ? OFPort.OFPP_FLOOD.getValue() :
                   outPort));
               List<OFAction> actions = new ArrayList<OFAction>();
               actions.add(action);
               po.setActions(actions);
               po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

               // set data if needed
               if (bufferId == 0xffffffff) {
                   byte[] packetData = pi.getPacketData();
                   po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH +
                           po.getActionsLength()+packetData.length));
                   po.setPacketData(packetData);
               } else {
                   po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH +
                           po.getActionsLength()));
               }
               stream.write(po);
            }
        }

        public String toString() {
            InetAddress remote = sock.socket().getInetAddress();
            return remote.getHostAddress() + ":" + sock.socket().getPort();
        }

        public OFMessageAsyncStream getStream() {
            return stream;
        }
    }

    public Controller(int port) throws IOException{
        listenSock = ServerSocketChannel.open();
        listenSock.configureBlocking(false);
        listenSock.socket().bind(new java.net.InetSocketAddress(port));
        listenSock.socket().setReuseAddress(true);

        switchSelectLoops = new ArrayList<SelectLoop>();
        switchSockets = new ConcurrentHashMap<SocketChannel,OFSwitch>();
        threadCount = 1;
        listenSelectLoop = new SelectLoop(this);
        // register this connection for accepting
        listenSelectLoop.register(listenSock, SelectionKey.OP_ACCEPT, listenSock);

        this.factory = new BasicFactory();
    }

    @Override
    public void handleEvent(SelectionKey key, Object arg) throws IOException {
        if (arg instanceof ServerSocketChannel)
            handleListenEvent(key, (ServerSocketChannel)arg);
        else
            handleSwitchEvent(key, (SocketChannel) arg);
    }

    protected void handleListenEvent(SelectionKey key, ServerSocketChannel ssc)
            throws IOException {
        SocketChannel sock = listenSock.accept();
        OFMessageAsyncStream stream = new OFMessageAsyncStream(sock, factory);
        switchSockets.put(sock, new OFSwitch(sock, stream));
        System.err
                .println("Got new connection from " + switchSockets.get(sock));
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
        sl.register(sock, ops, sock);
        // force select to return and re-enter using the new set of keys
        sl.wakeup();
    }

    protected void handleSwitchEvent(SelectionKey key, SocketChannel sock) {
        OFSwitch sw = switchSockets.get(sock);
        OFMessageAsyncStream stream = sw.getStream();
        try {
            if (key.isReadable()) {
                List<OFMessage> msgs = stream.read();
                if (msgs == null) {
                    key.cancel();
                    switchSockets.remove(sock);
                    return;
                }

                for (OFMessage m : msgs) {
                    switch (m.getType()) {
                        case PACKET_IN:
                            sw.handlePacketIn((OFPacketIn) m);
                            break;
                        case HELLO:
                            System.err.println("GOT HELLO from " + sw);
                            break;
                        case ECHO_REQUEST:
                            stream.write(stream.getMessageFactory()
                                    .getMessage(OFType.ECHO_REPLY));
                            break;
                        default:
                            System.err.println("Unhandled OF message: "
                                    + m.getType() + " from "
                                    + sock.socket().getInetAddress());
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
            switchSockets.remove(sock);
        }
    }

    public void run() throws IOException{
        // Static number of threads equal to processor cores
        es = Executors.newFixedThreadPool(threadCount);

        // Launch one select loop per threadCount and start running
        for (int i = 0; i < threadCount; ++i) {
            final SelectLoop sl = new SelectLoop(this);
            switchSelectLoops.add(sl);
            es.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        sl.doLoop();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }}
            );
        }

        // Start the listen loop
        listenSelectLoop.doLoop();
    }

    public static void main(String [] args) throws IOException {
        CommandLine cmd = parseArgs(args);
        int port = 6633;
        if (cmd.hasOption("p")) {
            port = Integer.valueOf(cmd.getOptionValue("p"));
        }

        Controller sc = new Controller(port);
        if (cmd.hasOption("t"))
            sc.threadCount = Integer.valueOf(cmd.getOptionValue("t"));
        sc.run();
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

    @Override
    public void addListener(OFType type, IOFMessageListener<?> listener) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void removeListener(OFType type, IOFMessageListener<?> listener) {
      // TODO Auto-generated method stub
      
    }
}
