<%@ page contentType="text/html;charset=UTF-8" %>
<div id="totalSelectST" class="mainback" style="display: none">
	<div class="row" id="row1">
		<div class="col-md-7">
			<table id="selectStudentTable" class="table">
				<thead>
					<tr>
						<th><g:checkBox name="overall" value="" checked="false" /></th>
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
							<td class="name">
								${s.firstname} ${s.lastname}
							</td>

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
		<div class="col-md-2" style="padding-top: 10%;">
			<div class="btn-group-vertical">
				<div class="btn btn-warning" id="add">Add</div>
				<div class="btn btn-warning" id="remove">Remove</div>
			</div>
		</div>

		<div id="selectedStudent" class="col-md-3">
			<div class="panel">
				<div class="panel-heading"><g:checkBox name="stList" value=" "
									checked="false" /> Selected Student</div>
				<div class="panel-body">
					<ul id="selectedList"></ul>
				</div>
			</div>
		</div>

	</div>
	
	<!-- Row End -->
	<div class="row col-md-offset-6">
		<g:hiddenField name="idList" value=""/>
		<div class="btn btn-default" id="totalCreate">Create</div>
		<div class="btn btn-default" id="totalPrevious">Cancel</div>
	</div>
</div>