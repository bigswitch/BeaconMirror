<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="net.beaconcontroller.topology.LinkTuple, 
                 net.beaconcontroller.topology.IdPortTuple,
                 java.util.Date,
                 java.util.Map"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<div class="section">
  <div class="section-header">${title}</div>
  <div class="section-content">
    <table class="beaconTable">
      <thead>
        <tr>
          <th>Link</th>
          <th>Last Seen</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach items="${links}" var="entry" varStatus="status">
          <%  LinkTuple linkTuple = (LinkTuple)((Map.Entry)pageContext.findAttribute("entry")).getKey();
              Long lastSeenL = (Long)((Map.Entry)pageContext.findAttribute("entry")).getValue();
              pageContext.setAttribute("linkTuple", linkTuple.toString());
              pageContext.setAttribute("lastSeen", new Date(lastSeenL.longValue()));
          %>
          <tr>
            <td><c:out value="${linkTuple}"/></td>
            <td><fmt:formatDate value="${lastSeen}" pattern="MM/dd HH:mm:ss"/></td>
           </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>