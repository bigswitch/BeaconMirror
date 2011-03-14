package net.beaconcontroller.topology.dao.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.beaconcontroller.storage.CompoundPredicate;
import net.beaconcontroller.storage.IResultSet;
import net.beaconcontroller.storage.IStorageSource;
import net.beaconcontroller.storage.OperatorPredicate;
import net.beaconcontroller.topology.dao.DaoSwitchPortTuple;
import net.beaconcontroller.topology.dao.DaoLinkTuple;
import net.beaconcontroller.topology.dao.ITopologyDao;

import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSTopologyDao implements ITopologyDao {

    protected static Logger logger = LoggerFactory.getLogger(SSTopologyDao.class);
    protected IStorageSource storageSource;

    private static final String LINK_TABLE_NAME = "controller_link";
    private static final String LINK_ID = "id";
    private static final String LINK_SRC_SWITCH = "src_switch_id";
    private static final String LINK_SRC_PORT = "src_port";
    private static final String LINK_SRC_PORT_STATE = "src_port_state";
    private static final String LINK_DEST_SWITCH = "dst_switch_id";
    private static final String LINK_DEST_PORT = "dst_port";
    private static final String LINK_DST_PORT_STATE = "src_port_state";
    private static final String LINK_VALID_TIME = "valid_time";
    
    public void setStorageSource(IStorageSource storageSource) {
        this.storageSource = storageSource;
        storageSource.createTable(LINK_TABLE_NAME);
        storageSource.setTablePrimaryKeyName(LINK_TABLE_NAME, LINK_ID);
    }

    @Override
    public void clear() {
        IResultSet resultSet = storageSource.executeQuery(LINK_TABLE_NAME, null, null, null);
        while (resultSet.next())
            resultSet.deleteRow();
        resultSet.save();
        resultSet.close();
    }

    private String getLinkId(DaoLinkTuple lt) {
        String srcDpid = HexString.toHexString(lt.getSrc().getId());
        String dstDpid = HexString.toHexString(lt.getDst().getId());
        return srcDpid + "-" + lt.getSrc().getPort() + "-" +
            dstDpid + "-" + lt.getDst().getPort();
    }
    
    @Override
    public void addLink(DaoLinkTuple lt, Long timeStamp) {
        Map<String, Object> rowValues = new HashMap<String, Object>();
        
        String id = getLinkId(lt);
        rowValues.put(LINK_ID, id);
        String srcDpid = HexString.toHexString(lt.getSrc().getId());
        rowValues.put(LINK_SRC_SWITCH, srcDpid);
        rowValues.put(LINK_SRC_PORT, lt.getSrc().getPort());
        String dstDpid = HexString.toHexString(lt.getDst().getId());
        rowValues.put(LINK_DEST_SWITCH, dstDpid);
        rowValues.put(LINK_DEST_PORT, lt.getDst().getPort());
        rowValues.put(LINK_VALID_TIME, timeStamp);
        storageSource.updateRow(LINK_TABLE_NAME, rowValues);
    }

    @Override
    public void updateLink(DaoLinkTuple lt, Long timeStamp) {
        Map<String, Object> rowValues = new HashMap<String, Object>();
        String id = getLinkId(lt);
        rowValues.put(LINK_ID, id);
        rowValues.put(LINK_VALID_TIME, timeStamp);
        storageSource.updateRow(LINK_TABLE_NAME, id, rowValues);
    }

    @Override
    public Long getLink(DaoLinkTuple lt) {
        String[] columns = { LINK_VALID_TIME };
        String id = getLinkId(lt);
        IResultSet resultSet = storageSource.executeQuery(LINK_TABLE_NAME, columns,
                new OperatorPredicate(LINK_ID, OperatorPredicate.Operator.EQ, id), null);
        if (!resultSet.next())
            return null;
        Long validTime = resultSet.getLong(LINK_VALID_TIME);
        return validTime;
    }

    private DaoLinkTuple readDaoLinkTuple(IResultSet resultSet) {
        String srcDpidString = resultSet.getString(LINK_SRC_SWITCH);
        Long srcDpid = HexString.toLong(srcDpidString);
        Short srcPortNumber = resultSet.getShortObject(LINK_SRC_PORT);
        Integer srcPortState = resultSet.getIntegerObject(LINK_SRC_PORT_STATE);
        String dstDpidString = resultSet.getString(LINK_DEST_SWITCH);
        Long dstDpid = HexString.toLong(dstDpidString);
        Short dstPortNumber = resultSet.getShort(LINK_DEST_PORT);
        Integer dstPortState = resultSet.getIntegerObject(LINK_DST_PORT_STATE);
        DaoLinkTuple lt = new DaoLinkTuple(srcDpid, srcPortNumber, srcPortState,
                dstDpid, dstPortNumber, dstPortState);
        return lt;
    }
    
    @Override
    public Set<DaoLinkTuple> getLinks(Long id) {
        Set<DaoLinkTuple> results = new HashSet<DaoLinkTuple>();
        String idString = HexString.toHexString(id);
        IResultSet resultSet = storageSource.executeQuery(LINK_TABLE_NAME, null,
                new CompoundPredicate(CompoundPredicate.Operator.OR, false,
                        new OperatorPredicate(LINK_SRC_SWITCH, OperatorPredicate.Operator.EQ, idString),
                        new OperatorPredicate(LINK_DEST_SWITCH, OperatorPredicate.Operator.EQ, idString)), null);
        for (IResultSet nextResult : resultSet) {
            DaoLinkTuple lt = readDaoLinkTuple(nextResult);
            results.add(lt);
        }
        return results.size() > 0 ? results : null;
    }

    @Override
    public Set<DaoLinkTuple> getLinks(DaoSwitchPortTuple idPort) {
        Set<DaoLinkTuple> results = new HashSet<DaoLinkTuple>();
        String idString = HexString.toHexString(idPort.getId());
        IResultSet resultSet = storageSource.executeQuery(LINK_TABLE_NAME, null,
                new CompoundPredicate(CompoundPredicate.Operator.OR, false,
                        new CompoundPredicate(CompoundPredicate.Operator.AND, false,
                                new OperatorPredicate(LINK_SRC_SWITCH, OperatorPredicate.Operator.EQ, idString),
                                new OperatorPredicate(LINK_SRC_PORT, OperatorPredicate.Operator.EQ, idPort.getPort())),
                        new CompoundPredicate(CompoundPredicate.Operator.AND, false,
                                new OperatorPredicate(LINK_DEST_SWITCH, OperatorPredicate.Operator.EQ, idString),
                                new OperatorPredicate(LINK_DEST_PORT, OperatorPredicate.Operator.EQ, idPort.getPort()))), null);
        for (IResultSet nextResult : resultSet) {            
            DaoLinkTuple lt = readDaoLinkTuple(nextResult);
            results.add(lt);
        }
        return results.size() > 0 ? results : null;
    }

    @Override
    public Set<DaoLinkTuple> getLinksToExpire(Long deadline) {
        Set<DaoLinkTuple> results = new HashSet<DaoLinkTuple>();
        IResultSet resultSet = storageSource.executeQuery(LINK_TABLE_NAME, null,
                new OperatorPredicate(LINK_VALID_TIME, OperatorPredicate.Operator.LTE, deadline), null);
        for (IResultSet nextResult : resultSet) {
            DaoLinkTuple lt = readDaoLinkTuple(nextResult);
            results.add(lt);
        }
        return results.size() > 0 ? results : null;
    }

    @Override
    public void removeLink(DaoLinkTuple lt) {
        String id = getLinkId(lt);
        storageSource.deleteRow(LINK_TABLE_NAME, id);
    }

    @Override
    public Set<DaoLinkTuple> removeLinksBySwitch(Long id) {
        Set<DaoLinkTuple> deleteSet = getLinks(id);
        if (deleteSet != null) {
            for (DaoLinkTuple lt : deleteSet) {
                removeLink(lt);
            }
        }
        return deleteSet;
    }

    @Override
    public Set<DaoLinkTuple> removeLinksBySwitchPort(DaoSwitchPortTuple idPort) {
        Set<DaoLinkTuple> deleteSet = getLinks(idPort);
        if (deleteSet != null) {
            for (DaoLinkTuple lt : deleteSet) {
                removeLink(lt);
            }
        }
        return deleteSet;
    }

}
