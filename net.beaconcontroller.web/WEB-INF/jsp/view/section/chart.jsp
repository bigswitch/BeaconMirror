<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<div class="beaconChart">
  <p>beacon chart</p>
        <c:forEach items="${chartParams}" var="chartParamEntry">
          <param name="${chartParamEntry.key}" value ="${chartParamsEntry.value}"/>
        </c:forEach>
</div>
