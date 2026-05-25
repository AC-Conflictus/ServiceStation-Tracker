<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<asset:javascript src="aoMain.js" />
</head>
<body>
	<form>
		<div class="row greyBar">

			<div class="col-md-6 navbar-right cornerButtons"
				style="margin-top: 7px; margin-bottom: -7px;">
				<g:link controller="commOrg" action="_createAgency" update="main">
					<span style="color: #FFDE97" class="glyphicon glyphicon-plus"
						aria-hidden="true"></span>
					<b>Add</b>
				</g:link>
				<span style="color: #FFDE97" class="glyphicon glyphicon-trash"
					aria-hidden="true"></span>
				<g:submitToRemote controller="commOrg" action="deletemult"
					value="Delete" class="tabledelete"></g:submitToRemote>
			</div>
		</div>
		<!-- Student Table -->
		<table id="table" class="table">
			<thead>
				<tr>
					<th style="background-color: #C53C3E; border: none"><input
						type="checkbox"></th>
					<th>Name</th>

					<th>Description</th>

					<th>Address</th>

					<th>Contact</th>

					<th>Contact Phone</th>

					<th>Contact Email</th>

					<th></th>

				</tr>
			</thead>
			<tbody>
				<g:each in="${list}" status="i" var="ag">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'} aoRow" id="${ag.id}">

						<td><g:checkBox name="checkAgency" value="${ag.id}"
								checked="false" /></td>

						<td id="agency">
							${ag.name }
						</td>

						<td>
							${ag.description }
						</td>

						<td>
							${ag.address}
						</td>

						<td>
							${ag.contact}
						</td>

						<td>
							${ag.contactPhone}
						</td>

						<td>
							${ag.contactEmail}
						</td>

						<td class="aoTableIcon"><g:remoteLink controller="commOrg"
								action="_editAgency" update="agMain" id="${ag.id}">
								<span class="glyphicon glyphicon-edit" aria-hidden="true"></span>
							</g:remoteLink> <a id="deleteAgOnTable" onclick="deleteConfirm(${ag.id})"> <span
								class="glyphicon glyphicon-trash" aria-hidden="true"></span>
						</a></td>

					</tr>
				</g:each>
			</tbody>
		</table>

	</form>
	<script>
	$(function() {
		$("#table").DataTable({
			"columnDefs" : [ {
				"orderable" : false,
				"targets" : [ 0, 7 ]
			} ]
		});
	});
	
	function deleteConfirm(id){
		result = confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');
		if (result){
			jQuery.ajax({
			type:'POST', url:'/sstation/commOrg/_deleteOnTable/'+id,
			success:function(data,textStatus){
				jQuery('#agMain').html(data);
			},
			error:function(XMLHttpRequest,textStatus,errorThrown){}});
		
		} 
	}
  $(document).ready(function(){
	    $('#table').DataTable();
	    $("#deleteAgOnTable").mouseenter(function(){
	    	$("#deleteAgOnTable").css("cursor","pointer");
	    	   });
	        
	});
  </script>
</body>
</html>