<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<div class="mainback">
	<p style="font-weight: 700; font-size: 15px">
		Right now you can only select one student. <br /> The display list
		does not work<br /> The remove button does not work</br> Procedures: <br />1.
		Click on the checkbox associated with one student <br />2. Click "add"
		button <br />3. Click "link" button
	</p>
	<div class="row" id="row1">
		<div class="col-md-8">
			<table id="selectStudentTable" class="table">
				<thead>
					<tr>
						<th><input type="checkbox"></th>
						<th>AC ID</th>

						<th>Name</th>
						<th>AC Box</th>

						<th>Email</th>

						<th>Clas</th>

						<th>AC Year</th>

					</tr>
				</thead>
				<tbody>
					<g:each in="${list}" status="i" var="s">
						<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
							<td><g:checkBox name="checkstudent" value="${s.id}"
									checked="false" /></td>

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
		</div>
		<div class="col-md-1" style="padding-top: 10%;">
			<div class="btn-group-vertical">
				<div class="btn btn-warning" id="add">Add</div>
				<div class="btn btn-warning" id="remove">Remove</div>
			</div>
		</div>

		<div id="selectedStudent" class="col-md-3">
			<div class="panel panel-warning">
				<div class="panel-heading">Selected Student</div>
				<div class="panel-body">
					<ul id="selectedList"></ul>
				</div>
			</div>
		</div>

	</div>
	<!-- Row End -->
	<div class="row col-md-offset-6">
		<div class="btn btn-default" id="selectedSt">Link</div>
		<g:remoteLink update="totalhour" controller="acStudent" action="_totalHourEdit">Previous</g:remoteLink>
	</div>
</div>
<script>
$(function(){
$("#selectStudentTable").DataTable();
var ss=new Array();	
$("#add").click(function(){
	var checkedVals = $("input[name='checkstudent']:checked").map(function() {
		return $(this).val();
		}).get();
	
	for(var i=0;i<checkedVals.length;i++){
		var id=parseInt(checkedVals[i]);
		ss.push(id);
		var string="<li id='"+id+"'><input type='checkbox' name='list' value='' checked='false'>Student"+id+"</li>";
		$("#selectedList").append(string);
		}
	alert(ss);
	
});
		
$("#selectedSt").click(function(){
	$("#idList").val(ss);
	alert($("#idList").val());
	$("#selectS").dialog("close");
});
});
</script>
</html>