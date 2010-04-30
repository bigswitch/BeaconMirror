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
public class EthernetTest extends TestCase {
    public void testToMACAddress() {
        byte[] address = new byte[] { 0x0, 0x11, 0x22, (byte) 0xff,
                (byte) 0xee, (byte) 0xdd};
        assertTrue(Arrays.equals(address, Ethernet
                .toMACAddress("00:11:22:ff:ee:dd")));
        assertTrue(Arrays.equals(address, Ethernet
                .toMACAddress("00:11:22:FF:EE:DD")));
    }

    public void testSerialize() {
        Ethernet ethernet = new Ethernet()
            .setDestinationMACAddress("de:ad:be:ef:de:ad")
            .setSourceMACAddress("be:ef:de:ad:be:ef")
            .setEtherType((short) 0);
        byte[] expected = new byte[] { (byte) 0xde, (byte) 0xad, (byte) 0xbe,
                (byte) 0xef, (byte) 0xde, (byte) 0xad, (byte) 0xbe,
                (byte) 0xef, (byte) 0xde, (byte) 0xad, (byte) 0xbe,
                (byte) 0xef, 0x0, 0x0 };
        assertTrue(Arrays.equals(expected, ethernet.serialize()));
    }
}
