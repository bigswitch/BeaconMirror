package net.beaconcontroller.topology;

public interface ITopology {
    /**
     * Query to determine if the specified switch id and port tuple are
     * connected to another switch or not.  If so, this means the link
     * is passing LLDPs properly between two OpenFlow switches.
     * @param idPort
     * @return
     */
    public boolean isInternal(IdPortTuple idPort);
}
