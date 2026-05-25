<div id="totalSelectST" class="mainback">
		<div class="row" id="row1">
			<div class="col-md-12">
				<table id="selectStudentTable" class="table">
					<thead>
						<tr>
							<th></th>
							<th>Name</th>
							<th>AC ID</th>
							<th>Classification</th>
						</tr>
					</thead>
					<tbody>
						<g:each in="${list}" status="i" var="s">
							<tr>
								<td><g:checkBox name="checkstudent" value="${s.id}"
										checked="false" /></td>
								<td class="name">${s.firstname} ${s.lastname}</td>

								<td id="studentid">
									${s.acid}
								</td>

								<td>
									${s.classification}
								</td>

							</tr>
						</g:each>
					</tbody>
				</table>
			</div>
			

	

		</div>
		
		<!-- Row End -->
		
		
		<div class="row col-md-offset-6">
			<div class="btn btn-default" id="totalPrevious">Cancel</div>
			<div class="btn btn-default" id="selectStud">Select</div>
			
		</div>
		
</div>
<script>
$(function(){
	$("#selectStudentTable").DataTable();

	$("input[name='checkstudent']").change( function (){

		$("input[name='checkstudent']").not(this).prop('checked', false);


		});
});
</script>