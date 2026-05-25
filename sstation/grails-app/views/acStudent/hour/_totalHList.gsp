<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<div class="table_buttons" style="height: 2em">
			<div class="navbar-right cornerButtons" style="margin: 0px">
				<g:remoteLink controller="acStudent" params="[place:'total']"
					action="_totalCreateShour" update="totalhour">
					<span style="color: #FFDE97" class="glyphicon glyphicon-plus"
						aria-hidden="true"></span>
					<b> New Service Hour</b>
				</g:remoteLink>
			</div>
		</div>



		<table class="table table-bordered">
			<thead>
				<tr style="background-color: #C53C3E; border: none">
					<th><input type="checkbox"></th>
					<th>Service Event</th>
					<th>Campus Group</th>
					<th>Community Organization</th>
					<th>Start Time</th>
					<th>Duration</th>
					<th>Status</th  >
				</tr>
			</thead>
			<tbody>
				<g:each in="${list}" status="i" var="sh">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
						<td><g:checkBox name="checkstudent" value="${sh.id}"
								checked="false" /></td>
						<td id="sh.id"><g:remoteLink params="[place:'total']"
								controller="acStudent" action="_shourTotal" id="${sh.id}"
								update="totalhour">
								${sh.event}
							</g:remoteLink></td>
						<td>
							${sh.campusOrg?.name}
						</td>
						<td>
							${sh.commAg}
						</td>
						<td>
							${sh.starttime.format('MM/dd/yyyy HH:mm')}
						</td>
						<td>
							${sh.duration}
						</td>
						<td class="status" id="${sh.id}"> <g:if test="${sh.status.toString()=="APPROVED"}">
								<span class="label label-success htableLabel"> ${sh.status}
								</span>
							</g:if> <g:elseif test="${sh.status.toString()=="PENDING" }">
								<span class="label label-warning htableLabel"
									> ${sh.status}
								</span>
							</g:elseif> <g:elseif test="${sh.status.toString()=="REJECTED"}">
								<span class="label label-default htableLabel"
									> ${sh.status}
								</span>
							</g:elseif></td>

					</tr>
				</g:each>
			</tbody>
		</table>
		<script>
  $(document).ready(function(){
	    $('table').DataTable();
	});
  </script>
</html>