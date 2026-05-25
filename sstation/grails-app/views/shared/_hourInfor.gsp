<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="sstation.Status" %>
<ol class="property-list acStudent">

			<g:if test="${shour?.acStudent}">
				<li class="fieldcontain"><b>Student Name: </b> ${shour.acStudent.firstname} ${shour.acStudent.lastname}
				</li>
			</g:if>

			<g:if test="${shour?.event}">
				<li class="fieldcontain"><b>Event: </b> ${shour.event.name}
				</li>
			</g:if>

			<g:if test="${shour?.description}">
				<li class="fieldcontain"><b>Description: </b>${shour.description}
				</li>
			</g:if>

			<g:if test="${shour?.campusOrg?.name}">
				<li class="fieldcontain"><b>Campus Group: </b> ${shour.campusOrg?.name}
				</li>
			</g:if>
			<g:elseif test="${shour?.otherCamOrg}">
				<li class="fieldcontain"><b>Campus Group: </b> ${shour.otherCamOrg}
				</li>
			</g:elseif>
			
			<g:if test="${shour?.commAg}">
				<li class="fieldcontain"><b>Community Organization: </b>${shour.commAg.name}
				</li>
			</g:if>
			<g:elseif test="${shour?.otherCommAg}">
				<li class="fieldcontain"><b>Community Organization: </b> ${shour.otherCommAg}
				</li>
			</g:elseif>

			<g:if test="${shour?.starttime}">
				<li class="fieldcontain"><b>Start Time: </b> ${shour.starttime.format('MM/dd/yyyy HH:mm')}
				</li>
			</g:if>

			<g:if test="${shour?.duration}">
				<li class="fieldcontain"><b>Duration: </b>${shour.duration} hr
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
						test="${shour.status==Status.APPROVED}">
						<span class="label label-success htableLabel" style="width:100px">
							${shour.status}
						</span>
					</g:if> <g:elseif test="${shour.status==Status.PENDING }">
						<span class="label label-warning htableLabel" style="width:100px">
							${shour.status}
						</span>
					</g:elseif> <g:elseif test="${shour.status==Status.REJECTED}">
						<span class="label label-default htableLabel" style="width:100px">
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