/**
 * 
 */
package net.beaconcontroller.packet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author David Erickson (daviderickson@cs.stanford.edu)
 *
 */
public class IPv4Test {
    @Test
    public void testToIPv4Address() {
        int expected = 0xc0a80001;
        assertEquals(expected, IPv4.toIPv4Address("192.168.0.1"));
    }

    @Test
    public void testToIPv4AddressBytes() {
        byte[] expected = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        Assert.assertArrayEquals(expected, IPv4.toIPv4AddressBytes("255.255.255.255"));
        expected = new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80};
        Assert.assertArrayEquals(expected, IPv4.toIPv4AddressBytes("128.128.128.128"));
        expected = new byte[] {0x7f,0x7f,0x7f,0x7f};
        Assert.assertArrayEquals(expected, IPv4.toIPv4AddressBytes("127.127.127.127"));
    }

    @Test
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
