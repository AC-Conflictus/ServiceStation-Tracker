<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<div class="body">
  <!-- Title + Three buttons -->
		<div class="nav cornerButtons showButtons" role="navigation">
			<g:remoteLink controller="acStudent" params="[domain:2]" action="_editShour" id="${shour.id}" update="shour" style="padding-right:5px"><span style="color:#FFDE97" class="glyphicon glyphicon-edit" aria-hidden="true" ></span> Edit </g:remoteLink>
			<g:remoteLink controller="acStudent" params="[domain:2]" action="deleteShour" id="${shour.id}" update="shour"><span style="color:#FFDE97" class="glyphicon glyphicon-trash" aria-hidden="true"></span> Delete</g:remoteLink>

		</div>
		
		<!-- This place will show either the student information, the edit form, or the create form  -->
		<div id="show-sh" class="content scaffold-show mainback" role="main">
			<ol class="property-list acStudent">
			
				<g:if test="${shour?.event}">
				<li class="fieldcontain">
					Event: ${shour.event}					
				</li>
				</g:if>
				
				<g:if test="${shour?.description}">
				<li class="fieldcontain">
					Description: ${shour.description}	
				</li>
				</g:if>				
			
				<g:if test="${shour?.commOrg}">
				<li class="fieldcontain">
					Community Organization: ${shour.commOrg}					
				</li>
				</g:if>
				
				<g:if test="${shour?.starttime}">
				<li class="fieldcontain">
					Start Time: ${shour.starttime.format('MM/dd/yyyy')}
				</li>
				</g:if>	
				
				<g:if test="${shour?.duration}">
				<li class="fieldcontain">
					Duration: ${shour.duration}	
				</li>
				</g:if>	
			
				<g:if test="${shour?.eventContactName}">
				<li class="fieldcontain">
					Contact Name:${shour.eventContactName}
				</li>
				</g:if>
			
				<g:if test="${shour?.eventContactPhone}">
				<li class="fieldcontain">
					Contact Phone:${shour.eventContactPhone}					
				</li>
				</g:if>
			
				<g:if test="${shour?.eventContactEmail}">
				<li class="fieldcontain">
					Contact Email:${shour.eventContactName}
					
				</li>
				</g:if>
			
				<g:if test="${shour?.status}">
				<li class="fieldcontain">
					Status:${shour.status}
					
				</li>
				</g:if>
				
			
			</ol>
			</div>
		
  
  </div>
</html>