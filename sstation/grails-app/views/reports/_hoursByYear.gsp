<div>
		<table id="stationYearDetailstable" style="width: 100%">
			<thead>
				<tr>
					<th>Service Event</th>
					<th>Campus Group</th>
					<th>Community Organization</th>
					<th>Start Time</th>
					<th>Duration</th>
				</tr>
			</thead>
			<tbody>
				<g:each in="${list}" status="i" var="s">
					<tr>
						<td>
							${s.event?.name}
						
						<td>
							${
								s.campusOrg?.name
							}
						</td>

						<td>
							${s.commAg?.name}
						</td>

						<td>
							${s.starttime?.format('MM/dd/yyyy HH:mm')}
						</td>

						<td>
							${s.duration}
						</td>


					</tr>
				</g:each>
			</tbody>
		</table>
	</div>
<script>
$(function(){
	$("#stationYearDetailstable").DataTable();
});
</script>
