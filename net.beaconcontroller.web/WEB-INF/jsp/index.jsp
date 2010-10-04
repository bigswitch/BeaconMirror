<%@page import="org.openflow.util.HexString"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html>
    <head>
        <title>Beacon</title>
    </head>
    <body>
        <h1><%="Welcome to Beacon!"%></h1>
        <a href="bundles.do">Bundle List</a><br>
        Switches:<br>
        <table cellpadding="0" cellspacing="0">
            <tr>
                <td>DPID</td>
                <td>Connected Since</td>
            </tr>
            <c:forEach items="${switches}" var="switch">
                <tr>
                    <td>
                        <c:set value="${switch.id}" scope="page" var="switchId"/>
                        <%= HexString.toHexString((Long)pageContext.getAttribute("switchId")) %>
                    </td>
                    <td><c:out value="${switch.connectedSince}"/></td>
            </c:forEach>
        </table>
        <br>
        Listeners:<br>
        <table cellpadding="0" cellspacing="0">
            <tr>
                <td>OFType</td>
                <td>Listeners</td>
            </tr>
            <c:forEach items="${listeners}" var="entry">
                <tr>
                    <td><c:out value="${entry.key}"/></td>
                    <td> 
                        <c:forEach items="${entry.value}" var="listener">
                            <c:out value="${listener.name}"/>
                        </c:forEach>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </body>
</html>