/**
 *
 */
package net.beaconcontroller.devicemanager.internal;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.devicemanager.IDeviceManager;

/**
 * @author derickso
 *
 */
public class DeviceManagerImpl implements IDeviceManager, IOFMessageListener {
    protected IBeaconProvider beaconProvider;

    public void startUp() {
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
    }

    public void shutDown() {
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
    }

    @Override
    public String getName() {
        return "devicemanager";
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg) {
        return null;
    }

    /**
     * @param beaconProvider the beaconProvider to set
     */
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }
}
