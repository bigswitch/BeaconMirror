<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="net.beaconcontroller.core.IOFSwitch, org.openflow.util.HexString"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

    <table class="beaconTable">
      <thead>
        <tr>
          <th>Id</th>
          <th>Connected</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach items="${switches}" var="sw" varStatus="status">
          <%  IOFSwitch sw = (IOFSwitch)pageContext.findAttribute("sw"); 
              pageContext.setAttribute("hexId", HexString.toHexString(sw.getId()));
          %>
          <tr>
            <td><c:out value="${hexId}"/></td>
            <td><fmt:formatDate value="${sw.connectedSince}" pattern="MM/dd HH:mm:ss"/></td>
            <td>
              <a href="<c:url value="/wm/core/switch/${hexId}/flows"/>" class="beaconNewRefreshingTab" name="Flows">Flows</a>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
