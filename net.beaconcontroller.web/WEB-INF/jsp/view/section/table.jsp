<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<div class="section">
  <div class="section-header">${title}</div>
  <div class="section-content">
    <table class="beaconTable">
        <thead>
          <tr>
            <c:forEach items="${columnNames}" var="columnName">
                <th><c:out value="${columnName}"/></th>
            </c:forEach>
          </tr>
        </thead>
        <tbody>
            <c:forEach items="${cells}" var="row">
                <tr>
                    <c:forEach items="${row}" var="cell">
                        <td><c:out value="${cell}"/></td>
                    </c:forEach>
                </tr>
            </c:forEach>
        </tbody>
    </table>
  </div>
</div>