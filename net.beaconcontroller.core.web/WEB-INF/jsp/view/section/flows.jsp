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
          <th>In Port</th>
          <th>DL Src</th>
          <th>DL Dst</th>
          <th>DL Type</th>
          <th>NW Src</th>
          <th>NW Dst</th>
          <th>NW Protot</th>
          <th>TP Src</th>
          <th>TP Dst</th>
          <th>Wildcards</th>
          <th>Bytes</th>
          <th>Packets</th>
          <th>Time (s)</th>
          <th>Timeout (s)</th>
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
            <td><c:out value="${flow.match.inputPort}"/></td>
            <td><c:out value="${ethSrc}"/></td>
            <td><c:out value="${ethDst}"/></td>
            <td>
              <c:choose>
                <c:when test="${flow.match.dataLayerType == 2048}">IPv4</c:when>
                <c:when test="${flow.match.dataLayerType == 2054}">ARP</c:when>
                <c:when test="${flow.match.dataLayerType == 35020}">LLDP</c:when>
                <c:otherwise><c:out value="${flow.match.dataLayerType}"/></c:otherwise>
              </c:choose>
            </td>
            <td><c:out value="${ipSrc}"/></td>
            <td><c:out value="${ipDst}"/></td>
            <td>
              <c:choose>
                <c:when test="${flow.match.networkProtocol == 1}">ICMP</c:when>
                <c:when test="${flow.match.networkProtocol == 6}">TCP</c:when>
                <c:when test="${flow.match.networkProtocol == 17}">UDP</c:when>
                <c:otherwise><c:out value="${flow.match.networkProtocol}"/></c:otherwise>
              </c:choose>
            </td>
            <td><c:out value="${flow.match.transportSource}"/></td>
            <td><c:out value="${flow.match.transportDestination}"/></td>
            <td><c:out value="${flow.match.wildcards}"/></td>
            <td><c:out value="${flow.byteCount}"/></td>
            <td><c:out value="${flow.packetCount}"/></td>
            <td><c:out value="${flow.durationSeconds}"/></td>
            <td><c:out value="${flow.idleTimeout}"/></td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>