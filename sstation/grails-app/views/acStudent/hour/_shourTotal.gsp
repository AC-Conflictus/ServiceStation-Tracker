<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<!-- Show a specific service hour -->
<div class="body">
	<!-- Title + Three buttons -->
	<div class="nav cornerButtons showButtons" role="navigation">
		<g:remoteLink controller="acStudent" action="_editShour"
			id="${shour.id}" update="totalhour" params="[place:'total']" style="padding-right:5px">
			<span style="color: #FFDE97" class="glyphicon glyphicon-edit"
				aria-hidden="true"></span> Edit </g:remoteLink>
		<g:remoteLink controller="acStudent" action="deleteShour"
			id="${shour.id}" params="[place:'total']" update="totalhour">
			<span style="color: #FFDE97" class="glyphicon glyphicon-trash"
				aria-hidden="true"></span> Delete</g:remoteLink>

	</div>


	<!-- This place will show either the student information, the edit form, or the create form  -->
	<div id="show-sh" class="content scaffold-show mainback" role="main">
		<ol class="property-list acStudent">

			<g:if test="${shour?.event}">
				<li class="fieldcontain"><b>Event: </b> ${shour.event}
				</li>
			</g:if>

			<g:if test="${shour?.description}">
				<li class="fieldcontain"><b>Description: </b>${shour.description}
				</li>
			</g:if>

			<g:if test="${shour?.campusOrg?.name}">
				<li class="fieldcontain"><b>Campus Group: </b> ${shour.campusOrg.name}
				</li>
			</g:if>
			
			<g:if test="${shour?.commAg}">
				<li class="fieldcontain"><b>Community Organization: </b>${shour.commAg}
				</li>
			</g:if>

			<g:if test="${shour?.starttime}">
				<li class="fieldcontain"><b>Start Time: </b> ${shour.starttime.format('MM/dd/yyyy HH:mm')}
				</li>
			</g:if>

			<g:if test="${shour?.duration}">
				<li class="fieldcontain"><b>Duration: </b>${shour.duration}
				</li>
			</g:if>

			<g:if test="${shour?.eventContactName}">
				<li class="fieldcontain"><b>Contact Name: </b>${shour.eventContactName}
				</li>
			</g:if>

			<g:if test="${shour?.eventContactPhone}">
				<li class="fieldcontain"><b>Contact Phone: </b> ${shour.eventContactPhone}
				</li>
			</g:if>

			<g:if test="${shour?.eventContactEmail}">
				<li class="fieldcontain"><b>Contact Email: </b> ${shour.eventContactEmail}

				</li>
			</g:if>

			<g:if test="${shour?.status}">
				<li class="fieldcontain"><b>Status: </b><g:if
						test="${shour.status.toString()=="APPROVED"}">
						<span class="label label-success htableLabel"
							style="padding-left: 12px; padding-right: 12px;">
							${shour.status}
						</span>
					</g:if> <g:elseif test="${shour.status.toString()=="PENDING" }">
						<span class="label label-warning htableLabel">
							${shour.status}
						</span>
					</g:elseif> <g:elseif test="${shour.status.toString()=="REJECTED"}">
						<span class="label label-default htableLabel"
							style="padding-left: 12px; padding-right: 12px;">
							${shour.status}
						</span>
					</g:elseif>

				</li>
			</g:if>

			<g:if test="${shour?.lastmodified}">
				<li class="fieldcontain"><b>Last Modified: </b> ${shour?.lastmodified?.format('MM/dd/yyyy HH:mm')}

				</li>
			</g:if>


		</ol>
	</div>


</div>
</html>