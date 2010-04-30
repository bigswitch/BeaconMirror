/**
 * 
 */
package net.beaconcontroller.packet;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * @author derickso
 *
 */
public class IPv4Test extends TestCase {
    public void testToIPv4Address() {
        int expected = 0xc0a80001;
        assertEquals(expected, IPv4.toIPv4Address("192.168.0.1"));
    }

    public void testSerialize() {
        byte[] expected = new byte[] { 0x45, 0x00, 0x00, 0x14, 0x5e, 0x4e,
                0x00, 0x00, 0x3f, 0x06, 0x31, 0x2e, (byte) 0xac, 0x18,
                0x4a, (byte) 0xdf, (byte) 0xab, 0x40, 0x4a, 0x30 };
        IPv4 packet = new IPv4()
            .setIdentification((short) 24142)
            .setTtl((byte) 63)
            .setProtocol((byte) 0x06)
            .setSourceAddress("172.24.74.223")
            .setDestinationAddress("171.64.74.48");
        byte[] actual = packet.serialize();
        assertTrue(Arrays.equals(expected, actual));
    }
}
