<%@page import="org.openflow.protocol.statistics.OFFlowStatisticsReply"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.openflow.util.HexString, org.openflow.protocol.*,
                 net.beaconcontroller.packet.*"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<div class="section">
  <div class="section-header">${title}</div>
  <div class="section-content">
    <table class="beaconTable">
      <thead>
        <tr>
          <th>Eth Src</th>
          <th>Eth Dst</th>
          <th>IP Src</th>
          <th>IP Dst</th>
          <th>Bytes</th>
          <th>Packets</th>
          <th>Time (s)</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach items="${flows}" var="flow" varStatus="status">
          <%  OFFlowStatisticsReply flow = (OFFlowStatisticsReply)pageContext.findAttribute("flow"); 
              pageContext.setAttribute("ethSrc", HexString.toHexString(flow.getMatch().getDataLayerSource()));
              pageContext.setAttribute("ethDst", HexString.toHexString(flow.getMatch().getDataLayerDestination()));
              pageContext.setAttribute("ipSrc", IPv4.fromIPv4Address(flow.getMatch().getNetworkSource()));
              pageContext.setAttribute("ipDst", IPv4.fromIPv4Address(flow.getMatch().getNetworkDestination()));
          %>
          <tr>
            <td><c:out value="${ethSrc}"/></td>
            <td><c:out value="${ethDst}"/></td>
            <td><c:out value="${ipSrc}"/></td>
            <td><c:out value="${ipDst}"/></td>
            <td><c:out value="${flow.byteCount}"/></td>
            <td><c:out value="${flow.packetCount}"/></td>
            <td><c:out value="${flow.durationSeconds}"/></td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>