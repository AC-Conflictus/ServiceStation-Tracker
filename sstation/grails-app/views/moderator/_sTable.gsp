<form>
	<div class="row"
		style="background-color: #625D4C; margin: 20px 0px 0px 0px">

		<div class="col-md-6 navbar-right cornerButtons"
			style="margin-top: 7px; margin-bottom: -7px;">
			<g:remoteLink controller="moderator" action="_presentStudentTableAdd"
				update="tableStudent" class="tStudent">
				<span style="color: #FFDE97" class="glyphicon glyphicon-plus"
					aria-hidden="true"></span>
				<b>Add</b>
			</g:remoteLink>
			<g:remoteLink controller="moderator" action="_presentStudentTableDelete"
				update="tableStudent" class="tStudent">
				<span style="color: #FFDE97" class="glyphicon glyphicon-trash"
				aria-hidden="true"></span>
				<b>Delete</b>
			</g:remoteLink>
		</div>
	</div>
	<!-- Student Table -->
	<table id="table" class="table">
		<thead>
			<tr>

				<th>AC ID</th>

				<th>Name</th>

				<th>AC Box</th>

				<th>Email</th>

				<th>Classification</th>

				<th>AC Year</th>

			</tr>
		</thead>
		<tbody>
			<g:each in="${list}" status="i" var="s">
				<tr class="${(i % 2) == 0 ? 'even' : 'odd'} studentRow" id="${s.id}">

					<td id="studentid">
						${s.acid}
					</td>

					<td>
						${s.firstname} ${s.lastname}
					</td>

					<td>
						${s.acBox}
					</td>

					<td>
						${s.acEmail}
					</td>

					<td>
						${s.classification}
					</td>

					<td>
						${s.acYear}
					</td>

				</tr>
			</g:each>
		</tbody>
	</table>

</form>
<script>
  $(document).ready(function(){
	    $('#table').DataTable();

	    $("#tableStudent").dialog({
			dialogClass : 'no-close',
			autoOpen : false,
			resizable : true,
			height : 600,
			width : 800,
			modal : true,
			open: function(event, ui) { $(".ui-dialog-titlebar-close").hide(); }
		});

	    $(".tStudent").click(function() {
			$("#tableStudent").dialog("open");
		});

		

	});
  </script>