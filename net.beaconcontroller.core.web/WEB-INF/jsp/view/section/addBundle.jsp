<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.osgi.framework.Bundle, net.beaconcontroller.util.BundleState,
                 java.util.List, net.beaconcontroller.util.BundleAction"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>

<form method="post" action="/wm/core/bundle/add" enctype="multipart/form-data" class="beaconAjaxDialogForm">
  <table>
    <tr>
      <td>Select a bundle to upload (must be a .jar file): </td>
      <td><input type="file" name="file"/></td>
    </tr>
    <tr>
      <td colspan="2">
        <input type="submit" value="Upload"/>
      </td>
    </tr>
  </table>
</form>
