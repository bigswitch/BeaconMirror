package net.beaconcontroller.core.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.HashMap;
import java.util.Map;

import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.test.BeaconTestCase;

import org.openflow.protocol.OFType;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class ControllerTest extends BeaconTestCase {
    @SuppressWarnings("restriction")
    protected Controller getController() {
        return (Controller) getApplicationContext().getBean("controller");
    }

    @SuppressWarnings("restriction")
    public void testCallbackOrderingBase() throws Exception {
        Controller controller = getController();
        Map<String,String> callbackOrdering = new HashMap<String,String>();
        callbackOrdering.put("PACKET_IN", "test1,test2");
        controller.setCallbackOrdering(callbackOrdering);

        IOFMessageListener test1 = createMock(IOFMessageListener.class);
        expect(test1.getName()).andReturn("test1").atLeastOnce();
        IOFMessageListener test2 = createMock(IOFMessageListener.class);
        expect(test2.getName()).andReturn("test2").atLeastOnce();
        IOFMessageListener test3 = createMock(IOFMessageListener.class);
        expect(test3.getName()).andReturn("test3").atLeastOnce();

        replay(test1, test2, test3);
        controller.addListener(OFType.PACKET_IN, test1);
        controller.addListener(OFType.PACKET_IN, test2);
        controller.addListener(OFType.PACKET_IN, test3);

        verify(test1, test2, test3);
        assertEquals("test1", controller.getMessageListeners().get(OFType.PACKET_IN).get(0).getName());
        assertEquals("test2", controller.getMessageListeners().get(OFType.PACKET_IN).get(1).getName());
        assertEquals("test3", controller.getMessageListeners().get(OFType.PACKET_IN).get(2).getName());
    }
}
