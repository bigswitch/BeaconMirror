<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div class="section">
  <c:forEach items="${jsIncludes}" var="jsIncludePath">
    <script type="text/javascript">
      loadScript("${jsIncludePath}");
    </script>
  </c:forEach>
  <div class="section-header">${title}</div>
  <div class="section-content">
    <div id="${id}" class="${templateClass}">
      <c:forEach items="${templateParams}" var="templateParamEntry">
        <param name="${templateParamEntry.key}" value ="${templateParamEntry.value}"/>
      </c:forEach>
      <c:out value="${body}" escapeXml="${escapeXml}"/>
      
      <c:forEach items="${imports}" var="importPath">
        <c:import url="${importPath}"/>
      </c:forEach>
      
      
    </div>
  </div>
</div>