<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<table class="beaconTable">
    <thead>
      <tr>
        <c:forEach items="${columnNames}" var="columnName">
            <th><c:out value="${columnName}"/></th>
        </c:forEach>
      </tr>
    </thead>
    <tbody>
        <c:forEach items="${cells}" var="row" varStatus="columnCounter">
            <tr>
                <c:forEach items="${row}" var="cell" varStatus="columnCounter">
                  <!--  note the off -by-one problem between varStatus variables and array indeces -->
                  <c:set var="metadata" value="${columnMetadata[columnNames[columnCounter.count - 1]]}" scope="page"/>
                  <td><c:out value="${cell}" escapeXml="${metadata['escapeXML']}"/></td> 
                </c:forEach>
            </tr>
        </c:forEach>
    </tbody>
</table>
