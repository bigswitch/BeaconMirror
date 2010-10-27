/**
 * 
 */
package net.beaconcontroller.packet;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/**
 * @author David Erickson (daviderickson@cs.stanford.edu)
 *
 */
public class UDPTest {

    @Test
    public void testSerialize() {
        byte[] expected = new byte[] { 0x45, 0x00, 0x00, 0x1d, 0x56, 0x23,
                0x00, 0x00, (byte) 0x80, 0x11, 0x48, 0x7f, (byte) 0xc0,
                (byte) 0xa8, 0x01, 0x02, 0x0c, (byte) 0x81, (byte) 0xce, 0x02,
                0x17, (byte) 0xe1, 0x04, 0x5f, 0x00, 0x09, 0x46, 0x6e,
                0x01 };
        IPacket packet = new IPv4()
            .setIdentification((short) 22051)
            .setTtl((byte) 128)
            .setSourceAddress("192.168.1.2")
            .setDestinationAddress("12.129.206.2")
            .setPayload(new UDP()
                            .setSourcePort((short) 6113)
                            .setDestinationPort((short) 1119)
                            .setPayload(new Data(new byte[] {0x01}))
                       );
        byte[] actual = packet.serialize();
        assertTrue(Arrays.equals(expected, actual));
    }
}
