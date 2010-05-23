package net.beaconcontroller.core.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.IOFMessageListener.Command;
import net.beaconcontroller.test.BeaconTestCase;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class ControllerTest extends BeaconTestCase {
    protected Controller getController() {
        return (Controller) getApplicationContext().getBean("controller");
    }

    /**
     * Verify that our callbacks are ordered with respect to the order specified
     * @throws Exception
     */
    public void testCallbackOrderingBase() throws Exception {
        testCallbackOrdering(new String[] {"2"}, new String[] {"2"});
        testCallbackOrdering(new String[] {"3"}, new String[] {"3"});
        testCallbackOrdering(new String[] {"1","2"}, new String[] {"1","2"});
        testCallbackOrdering(new String[] {"2","1"}, new String[] {"1","2"});
        testCallbackOrdering(new String[] {"2","3"}, new String[] {"2","3"});
        testCallbackOrdering(new String[] {"3","2"}, new String[] {"2","3"});
        testCallbackOrdering(new String[] {"1","2","3"}, new String[] {"1","2","3"});
        testCallbackOrdering(new String[] {"1","3","2"}, new String[] {"1","2","3"});
        testCallbackOrdering(new String[] {"2","1","3"}, new String[] {"1","2","3"});
        testCallbackOrdering(new String[] {"2","3","1"}, new String[] {"1","2","3"});
        testCallbackOrdering(new String[] {"3","1","2"}, new String[] {"1","2","3"});
        testCallbackOrdering(new String[] {"3","2","1"}, new String[] {"1","2","3"});
    }

    protected void testCallbackOrdering(String[] addOrder, String[] verifyOrder) throws Exception {
        Controller controller = getController();
        controller.getMessageListeners().remove(OFType.PACKET_IN);
        Map<String,String> callbackOrdering = new HashMap<String,String>();
        callbackOrdering.put("PACKET_IN", "test1,test2");
        controller.setCallbackOrdering(callbackOrdering);

        IOFMessageListener test1 = createMock(IOFMessageListener.class);
        expect(test1.getName()).andReturn("test1").anyTimes();
        IOFMessageListener test2 = createMock(IOFMessageListener.class);
        expect(test2.getName()).andReturn("test2").anyTimes();
        IOFMessageListener test3 = createMock(IOFMessageListener.class);
        expect(test3.getName()).andReturn("test3").anyTimes();

        replay(test1, test2, test3);
        for (String o : addOrder) {
            if ("1".equals(o)) {
                controller.addListener(OFType.PACKET_IN, test1);
            } else if ("2".equals(o)) {
                controller.addListener(OFType.PACKET_IN, test2);
            } else {
                controller.addListener(OFType.PACKET_IN, test3);
            }
        }

        verify(test1, test2, test3);
        for (int i = 0; i < verifyOrder.length; ++i) {
            String o = verifyOrder[i];
            if ("1".equals(o)) {
                assertEquals("test1", controller.getMessageListeners().get(OFType.PACKET_IN).get(i).getName());
            } else if ("2".equals(o)) {
                assertEquals("test2", controller.getMessageListeners().get(OFType.PACKET_IN).get(i).getName());
            } else {
                assertEquals("test3", controller.getMessageListeners().get(OFType.PACKET_IN).get(i).getName());
            }
        }
    }

    /**
     * Verify that a listener can throw an exception and not ruin further
     * execution, and verify that the Commands STOP and CONTINUE are honored.
     * @throws Exception
     */
    public void testHandleMessages() throws Exception {
        Controller controller = getController();
        controller.getMessageListeners().remove(OFType.PACKET_IN);
        Map<String,String> callbackOrdering = new HashMap<String,String>();
        callbackOrdering.put("PACKET_IN", "test1,test2");
        controller.setCallbackOrdering(callbackOrdering);

        IOFSwitch sw = createMock(IOFSwitch.class);
        OFPacketIn pi = new OFPacketIn();
        IOFMessageListener test1 = createMock(IOFMessageListener.class);
        expect(test1.getName()).andReturn("test1").anyTimes();
        expect(test1.receive(sw, pi)).andThrow(new RuntimeException());
        IOFMessageListener test2 = createMock(IOFMessageListener.class);
        expect(test2.getName()).andReturn("test2").anyTimes();
        expect(test2.receive(sw, pi)).andReturn(Command.CONTINUE);

        replay(test1, test2, sw);
        controller.addListener(OFType.PACKET_IN, test1);
        controller.addListener(OFType.PACKET_IN, test2);
        controller.handleMessages(sw, Arrays.asList(new OFMessage[] {pi}));
        verify(test1, test2, sw);

        // verify STOP works
        reset(test1, test2, sw);
        expect(test1.receive(sw, pi)).andReturn(Command.STOP);
        replay(test1, test2, sw);
        controller.handleMessages(sw, Arrays.asList(new OFMessage[] {pi}));
        verify(test1, test2, sw);
    }
}
