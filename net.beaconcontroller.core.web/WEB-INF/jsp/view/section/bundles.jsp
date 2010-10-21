<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.osgi.framework.Bundle, net.beaconcontroller.util.BundleState,
                 java.util.List, net.beaconcontroller.util.BundleAction"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<div class="section">
  <div class="section-header">${title}</div>
  <div class="section-content">
    <table class="beaconTable">
        <thead>
          <tr>
            <th>Id</th>
            <th>Status</th>
            <th>Name</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
            <c:forEach items="${bundles}" var="entry" varStatus="status">
                <%  Bundle bundle = (Bundle)pageContext.findAttribute("entry"); 
                    BundleState state = BundleState.getState(bundle.getState());
                    List<BundleAction> actions = BundleAction.getAvailableActions(state);
                    pageContext.setAttribute("state", state);
                    pageContext.setAttribute("actions", actions);
                %>
                <tr>
                    <td><c:out value="${entry.bundleId}"/></td>
                    <td><c:out value="${state}"/></td>
                    <td><c:out value="${entry.symbolicName}"/>_<c:out value="${entry.version}"/></td>
                    <td>
                        <c:forEach items="${actions}" var="action">
                            <a href="<c:url value="/wm/core/bundle/${entry.bundleId}/${action}"/>" class="beaconRefreshTab"><c:out value="${action}"/></a>
                        </c:forEach>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
  </div>
</div>