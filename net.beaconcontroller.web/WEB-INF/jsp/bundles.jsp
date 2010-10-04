<%@page import="org.openflow.util.HexString"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html>
    <head>
        <title>Beacon</title>
    </head>
    <body>
        <h1><%="Welcome to Beacon!"%></h1>
        Bundles:<br>
        <table cellpadding="0" cellspacing="0">
            <tr>
                <td>Id</td>
                <td>Status</td>
                <td>Symbolic Name</td>
                <td>Version</td>
                <td>Action</td>
            </tr>
            <c:forEach items="${bundles}" var="entry" varStatus="status">
                <tr <c:if test="${status.count % 2 == 0}">bgcolor="#cdcdcd"</c:if>>
                    <td><c:out value="${entry.bundleId}"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${entry.state == 32}">Active</c:when>
                            <c:when test="${entry.state == 2}">Installed</c:when>
                            <c:when test="${entry.state == 4}">Resolved</c:when>
                            <c:when test="${entry.state == 8}">Starting</c:when>
                            <c:when test="${entry.state == 16}">Stopping</c:when>
                            <c:when test="${entry.state == 1}">Uninstalled</c:when>
                        </c:choose>
                    </td>
                    <td><c:out value="${entry.symbolicName}"/></td>
                    <td><c:out value="${entry.version}"/></td>
                    <td>
                        <a href="<c:url value="/bundle/${entry.bundleId}.do"><c:param name="action" value="start"/></c:url>">Start</a>
                        <a href="<c:url value="/bundle/${entry.bundleId}.do"><c:param name="action" value="stop"/></c:url>">Stop</a>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </body>
</html>