<div>
	<div class="row"
		style="background-color: #625D4C; margin: 20px 0px 0px 0px">

		<div class="col-md-6 navbar-right cornerButtons"
			style="margin-top: 7px; margin-bottom: -7px;">

			<g:link controller="acStudent" action="_createStudent" update="slist">


				<span style="color: #FFDE97" class="glyphicon glyphicon-plus"
					aria-hidden="true"></span> New Student
			
			</g:link>
			<span id="deleteStudentsBtn" style="color: #FFDE97"> <span
				class="glyphicon glyphicon-trash" aria-hidden="true"></span> Delete
			</span>



		</div>
	</div>

	<!-- Student Table -->
	<table id="table" class="table">
		<thead>
			<tr>
				<th style="background-color: #C53C3E; border: none"><input
					type="checkbox"></th>
				<th>AC ID</th>

				<th>Name</th>

				<th>AC Box</th>

				<th>Email</th>

				<th>Classification</th>

				<th>Status</th>

				<th>AC Year</th>

			</tr>
		</thead>
		<tbody>
			<g:each in="${list}" status="i" var="s">
				<tr class="${(i % 2) == 0 ? 'even' : 'odd'} studentRow" id="${s.id}">

					<td><g:checkBox name="checkstudent"
							value="${s.id}_${s.acid}_${s.firstname} ${s.lastname}"
							checked="false" /></td>

					<td class="studentid" id="${s.id}">
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
						${s.status }
					</td>

					<td>
						${s.acYear}
					</td>

				</tr>
			</g:each>
		</tbody>
	</table>

</div>


<div id="modal_Delete" class="modal" style="display: none;">

	<h1>Confirmation of Deleted Students</h1>

	<br>

	<table id="deleteStudentsTable" class="table">
		<thead>
			<tr>

				<th>AC ID</th>

				<th>Name</th>

			</tr>
		</thead>
		<tbody id="deleteBody">

		</tbody>
	</table>

	<%--<g:submitToRemote controller="acStudent" action="deletemult"
		update="main" value="Delete" class="tabledelete"></g:submitToRemote> --%>

</div>

<script>
	$(function() {
		$('#table').DataTable();
		$(document)
				.ready(
						function() {
							// $('#table').DataTable();
							var baseLink = '<g:createLink action="student" controller="acStudent" id="ID"/>';
							// var row = $(".studentRow");
							// window.alert(row.attr("id"));
							$(".studentRow....").on(
									{
										click : function() {
											//window.alert($(this).attr("acid"))
											var idstring = $(this).attr("id");

											window.location = baseLink.replace(
													"ID", idstring);
										}
									});

						});
		//$('#table').DataTable();

		var modalDelete = $("#modal_Delete").dialog({
			autoOpen : false,
			resizable : true,
			height : 500,
			width : 800,
			modal : true
		});

		$(".studentid")
				.click(
						function() {
							//var rows = $('#table').DataTable().rows;
							//window.alert(row.length);
							var acId = $(this).attr("id");
							var baseLink = '<g:createLink action="student" controller="acStudent" id="ID"/>';
							window.location = baseLink.replace("ID", acId);

						});

		$("#deleteStudentsBtn").on("mouseenter", function() {
			$("#deleteStudentsBtn").css("cursor", "pointer");
			$("#deleteStudentsBtn").css("text-decoration", "underline");
		});
		$("#deleteStudentsBtn").on("mouseleave", function() {
			$("#deleteStudentsBtn").css("text-decoration", "none");
		});
		$("#deleteStudentsBtn").on("click", function() {
			modalDelete.dialog("open");
		});

		$("input[name='checkstudent']").change(
				function() {

					var boo = $(this).prop('checked');
					//window.alert(boo);//sends boo as undefined... why?

					var info = $(this).val();
					//window.alert(info);

					var infoSet = info.split("_");
					var rowId = infoSet[0];
					var acId = infoSet[1];
					//window.alert(acId);
					var name = infoSet[2];

					if (boo) {
						var returnedInfo = "<tr id=dRow_"+acId+"><td>" + acId
								+ "</td><td>" + name + "</td></tr>";
						$('#deleteBody tr:last').after(returnedInfo);
					} else if (!boo) {
						$("#dRow_" + acId).remove();
					}
				});

		$("#deleteStudentsTable").DataTable();

	});
</script>