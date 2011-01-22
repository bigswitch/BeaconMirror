<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="net.beaconcontroller.topology.LinkTuple, 
                 net.beaconcontroller.topology.IdPortTuple,
                 java.util.Date,
                 java.util.Map"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<script type="text/javascript" src="js/jit-yc.js"></script>
<script type="text/javascript" src="js/bsnTopologyViewer.js"></script>
<div class="section">
  <div class="section-header">${title}</div>
  <div class="section-content">
    <p>some content</p>
    <div id='topologyCanvas' style="width:100%;height:400px"></div>
  </div>
</div>