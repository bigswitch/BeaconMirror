<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="java.util.HashMap, java.util.ArrayList" %> 

<style>
	#flowModTbl + .fg-toolbar {
		padding: 5px;
	}
	
	#flowModTbl th, #flowModTbl td {
		white-space: nowrap;
	}
	
	#addFlowModBtn {
		padding: 5px;
	}

	#center {
		line-height: 1.6em;
	}

	#center .section-header {
		line-height: 1.6em;
		padding: 5px;
	}
	
	#center .uniForm .buttonHolder {
		text-align: left;
		background-color: transparent;
		margin: 0;
		padding: 1.5em 0;
	}

	div.jGrowl-notification {
		float: right;
		margin-left: 6px;
	}
		
	div.jGrowl div.jGrowl-notification, div.jGrowl div.jGrowl-closer {
		background-color: #ffb;
		color: navy;
		padding: 4px;
		margin: 2px;
	}
	
	div.growlHdr {
		font-weight: bold;
		padding: 3px;
	}
	
	div.growlMsg {
		font-style: itallic;
		padding: 3px;
	}
	
	div.growlError {
		background-color: #FCC;
	}
	
	div#addFlowModPopup{
		display: none;
		opacity: 0;
		width: 800px;
		position: absolute; top: 40px; left: 170px;
		padding: 20px;
		border: solid 1px #bbb;
		background: #fff;
		-webkit-box-shadow: 0px 3px 6px rgba(0,0,0,0.25);
		-webkit-transition: -opacity 0.0s ease-out;
	}
	
	div#addFlowModPopup.show {
		display: block;
		opacity: 1.0;
		z-index: 100;
		-webkit-transition-duration: 0.5s;
	}
	
	div#closeFlowModPopup {
		float: right;
		cursor: pointer;
	}

	div#addFlowModPopupHdr {
		cursor: move;
 		padding: 10px 22px;
 		line-height: 2em;
 		background: #254a86;
 		border: 1px solid #163362;
 		font-size: 12px;
 		font-weight: bold;
 		color: #fff;
		border-radius: 4px;
		-webkit-border-radius: 4px;
		-moz-border-radius: 4px;
		-o-border-radius: 4px;
		-khtml-border-radius: 4px;
		box-shadow: 1px 1px 0 #fff;
		-webkit-box-shadow: 1px 1px 0 #fff;
		-moz-box-shadow: 1px 1px 0 #fff;
		text-shadow: -1px -1px 0 rgba(0,0,0,.25);
	}
	
	.uniForm label {
		display: inline-block;
		font-weight: bold;
		padding-right: 10px;
		text-align: right;
	}
	
	.uniForm .textInput {
		float: none;
	}
	
</style>

<div id='flowModFlash'></div>
<table id="flowModTbl" class="beaconTable">
    <thead>
      <tr>
	    <c:forEach items="${attrs}" var="attr" varStatus="row">
	        <th><c:out value="${attr}"/></th>
	    </c:forEach>        
      </tr>
    </thead>
    <tbody>
       <c:forEach items="${flowmods}" var="flowmod" varStatus="row">
            <tr>
                <td class="fm-active" style="text-align: center">
                    <input type="checkbox" class="activeCheckBox"
                			   data-name="<c:out value="${flowmod.name}"/>"
                			   data-switch="<c:out value="${flowmod.switch}"/>"
                			   data-json="<c:out value="${flowmod.json}"/>"
                			   <c:if test="${flowmod.active == 'true'}">
   								 <c:out value="checked" />
						   </c:if> /></td>
                <td class="fm-name" ><c:out value="${flowmod.name}"/></td>
                <td class="fm-switch" ><c:out value="${flowmod.switch}"/></td>
                <td class="fm-priority" ><c:out value="${flowmod.priority}"/></td>
                <td class="fm-cookie" ><c:out value="${flowmod.cookie}"/></td>
                <td class="fm-wildcards" ><c:out value="${flowmod.wildcards}"/></td>            
                <td class="fm-ingress-port" ><c:out value="${flowmod.ingress_port}"/></td>
                <td class="fm-vlan-id" ><c:out value="${flowmod.vlan_id}"/></td>
                <td class="fm-vlan-priority" ><c:out value="${flowmod.vlan_priority}"/></td>
                <td class="fm-ether-type" ><c:out value="${flowmod.ether_type}"/></td>
                <td class="fm-src-mac" ><c:out value="${flowmod.src_mac}"/></td>
                <td class="fm-dst-mac" ><c:out value="${flowmod.dst_mac}"/></td>               
                <td class="fm-protocol" ><c:out value="${flowmod.protocol}"/></td>
                <td class="fm-tos-bits" ><c:out value="${flowmod.tos_bits}"/></td>
                <td class="fm-src-ip" ><c:out value="${flowmod.src_ip}"/></td>
                <td class="fm-dst-ip" ><c:out value="${flowmod.dst_ip}"/></td>
                <td class="fm-src-port" ><c:out value="${flowmod.src_port}"/></td>
                <td class="fm-dst-port" ><c:out value="${flowmod.dst_port}"/></td>               
                <td class="fm-actions" ><c:out value="${flowmod.actions}"/></td>
           </tr>
        </c:forEach>
    </tbody>
</table>

<form class="uniForm">
	<div class="buttonHolder">
		<button id="addFlowModBtn" type="submit" class="primaryAction">Add New Flow Entry</button>
	</div>
</form>

<div id="addFlowModPopup" class="ui-widget-content">
	<div id="addFlowModPopupHdr">Add New Flow Entry<div id="closeFlowModPopup">X</div></div>
	<div id="addFlowModPopupBody">
		<form id="addFlowModPopupForm" action="#" class="uniForm">
		  <fieldset>
        			<div class="ctrlHolder">
			        <label for="name">name</label>
	          		<input type="text" id="name" name="name" value="" size="35" class="textInput" />
			        <label for="active">active</label>
	          		<input type="checkbox" id="active" name="active" class="activeCheckBox" />
		        </div>
		  </fieldset>

		<div style="width:48%; float:left">
		  <fieldset>
		    <c:forEach items="${attrs}" var="attr" varStatus="row">
                <c:if test="${row.index > 1 && row.index%2 == 0}">
        				<div class="ctrlHolder">
				        <label for="<c:out value="${attr}"/>"><c:out value="${attr}"/></label>
		          		<input type="text" id="<c:out value="${attr}"/>" name="<c:out value="${attr}"/>"
		          			   value="" size="35" class="textInput" />
	          		</div>
			    </c:if>
		    </c:forEach>
		  </fieldset>
		</div>
		
		<div style="width:48%; float:right">
		  <fieldset>
		    <c:forEach items="${attrs}" var="attr" varStatus="row">
                <c:if test="${row.index > 1 && row.index%2 == 1}">
        				<div class="ctrlHolder">
				        <label for="<c:out value="${attr}"/>"><c:out value="${attr}"/></label>
		          		<input type="text" id="<c:out value="${attr}"/>" name="<c:out value="${attr}"/>"
		          			   value="" size="35" class="textInput" />
	          		</div>
			    </c:if>
		    </c:forEach>
		  </fieldset>
		</div>
		  
		<div class="buttonHolder">
			<button id="saveFlowModBtn" type="submit" class="primaryAction">Save</button>
			<button id="applyFlowModBtn" type="submit" class="primaryAction">Apply</button>
		</div>
		</form>
	</div>
</div>

<script type="text/javascript"> 
	// Not using document.ready since this can be loaded as a fragment.
	
	function flowmodAttrs() {
		var ret = [
	    		<c:forEach items="${attrs}" var="attr" varStatus="row">
        			"<c:out value="${attr}"/>",
    			</c:forEach>
        		""];
		ret.pop();
		return ret;
	}

	function flowmodDecode(str){
	    return str.replace(/\&amp\;/g,'&').replace(/\&lt\;/g,'>').replace(/\&gt\;/g,'>').replace(/\&apos\;/g,"'").replace(/\&quot\;/g,'"');
	}

	function flowmodEncode(str) {
	    return str.replace(/\&/g,'&'+'amp;').replace(/</g,'&'+'lt;').replace(/>/g,'&'+'gt;').replace(/\'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');
	}

	function flowmodSuccess(fm, $this, responseText, textStatus, jqXHR) {
		var hdr = fm.name,
			msg = 'Set to ' + ((fm.active=="true")?'active':'inactive') + ' on ' + fm.switch;
		
		$.jGrowl('<div class="growlHdr">' + hdr + '</div>'+
				 '<div class="growlMsg">' + msg + '</div>',
				 {glue: 'before', position: 'center'});	
	}
		
	function flowmodFailure(fm, $this, responseText, textStatus, jqXHR) {
		var hdr = fm.name,
			msg = 'Failure in trying to set ' + ((fm.active=="true")?'active':'inactive') + ' on ' + fm.switch;
		
		if ($this.find('td').length > 0) {
			// We were trying to create a new flow-mod but failed. Remove it.
			$this.remove();
		}
		else {
			// We are 
			console.log(hdr, msg, textStatus, responseText);
			$this.attr('checked', !$this.attr('checked'));
			fm.active = $this.attr('checked').toString();
			$this.attr('data-json', JSON.stringify(fm));
		}
		
		$.jGrowl('<div class="growlHdr growlError">' + hdr + '</div>'+
				 '<div class="growlMsg">' + msg + '</div>',
				 {glue: 'before', position: 'center', sticky: true});	
	}
		
	function flowmodPush(fm, $this) {
		var posturl = '/wm/staticflowentry/pushentry.do?dpid=' + encodeURI(fm.switch),
		    data = {postBody: JSON.stringify(fm)};
		
		$.ajax({
			  type: 'POST',
			  url: posturl,
			  data: data,
			  dataType: "html",
			  success: function(responseText, textStatus, jqXHR) {
				  if (responseText == "OK") {
					  flowmodSuccess(fm, $this, responseText, textStatus, jqXHR);
				  }
				  else {
					  flowmodFailure(fm, $this, responseText, textStatus, jqXHR);
				  }
			  },
			  error: function(jqXHR, textStatus, errorThrown) {
				  flowmodFailure(fm, $this, errorThrown, textStatus, jqXHR);
			  }
		});
	}
	
	function flowmodValidate(fm) {
		if (!fm.name) return false;
		if (!fm.switch) return false;
		
		var found=false;
		$('#flowModTbl .fm-name').each(function() {
			if ($(this).text() == fm.name) {
				found = true;
			}
		});
		if (found) return false;
		
		return true;
	}
	
	function flowmodSave() {
		var fm = {},
			$form = $('#addFlowModPopupForm'),
			attrs = flowmodAttrs();
		
		for (i=0, ii=attrs.length; i<ii; i++) {
			var attr = attrs[i],
				val = $form.find('#'+attrs[i]).val();
			if (val) fm[attr] = val.toString(); // always string
		}
		fm['active'] = ($form.find('#active').attr('checked')) ? 'true':'false';
		
		if (!flowmodValidate(fm)) {
			// Validation error
			return false;
		}
		
		var $tr = $('<tr></tr>');
		$tr.append('<td class="fm-active" style="text-align: center">\
                   <input type="checkbox" class="activeCheckBox"\
               			   data-name="'+fm.name+'" data-switch="'+fm.switch+'"\
               			   data-json="'+flowmodEncode(JSON.stringify(fm))+'" ' +
               			   ((fm.active == 'true')?'checked':'') + ' /></td>');
		for (i=1, ii=attrs.length; i<ii; i++) {
			var attr = attrs[i],
				val = fm[attr];
			$tr.append('<td class="fm-'+attr+'">'+(val?val:'')+'</td>');
		}
		$('#flowModTbl').append($tr);
		
		flowmodPush(fm, $tr);
		$tr.find('.activeCheckBox').click(flowmodPushOnClick);
		return true;

	}

	function adjustLabelWidth() {
		setTimeout(function() {
			var labelWidth = 1;
			$('.uniForm label').each(function() {
				if ($(this).width() > labelWidth) {
					labelWidth = $(this).width();
				}
			});
			$('.uniForm label').width(labelWidth);			
		}, 1);
	}
	
	function flowmodPushOnClick(e) {
		var $this = $(this),
	    		fm = JSON.parse(flowmodDecode($this.attr('data-json')));
		fm.active = $this.attr('checked').toString();
		$this.attr('data-json', JSON.stringify(fm));
		flowmodPush(fm, $this);
		return true;
	}

    $.jGrowl.defaults.closer = false;
    $("#addFlowModPopup").draggable({handle: '#addFlowModPopupHdr'});
	$("#addFlowModBtn").click(function(e) {
		$("#addFlowModPopup").addClass("show");
		adjustLabelWidth();
		return false;
	});

	$("#saveFlowModBtn").click(function(e) {
		if (flowmodSave()) {
			$("#addFlowModPopup").removeClass("show");
		}
		else {
			// TODO: Need better validation and error messages
			alert('Error in flow-mod specification');
		}
		return false;
	});

	$("#applyFlowModBtn").click(function(e) {
		if (!flowmodSave()) {
			// Need better validation and error messages
			alert('Error in flow-mod specification');
		}
		return false;
	});

	$("#closeFlowModPopup").click(function(e) {
		$("#addFlowModPopup").removeClass("show");
		return false;
	});

	$("#flowModTbl .activeCheckBox").click(flowmodPushOnClick);
	
</script> 
