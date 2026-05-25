<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<asset:javascript src="aoMain.js" />
</head>
<body>
	<div class="table_buttons" style="height: 2em">
		<div class="col-md-3 navbar-right cornerButtons" style="margin: 0px;">
			<g:link controller="ACGroup" action="_createOrg" update="main">
				<span style="color: #FFDE97" class="glyphicon glyphicon-plus"
					aria-hidden="true"></span>
				<b> Add Campus Group</b>
			</g:link>
		</div>
	</div>
	<!-- Organization Table -->
	>
	<table id="otable" class="table" style="width: 100%">
		<thead>
			<tr>
				<th><input type="checkbox"></th>
				<th>Name</th>
				<th>Description</th>
				<th>Contact</th>
				<th>Contact Phone</th>
				<th>Contact Email</th>
				<th></th>
			</tr>
		</thead>
		<tbody>
			<g:each in="${list}" status="i" var="s">
				<tr class="${(i % 2) == 0 ? 'even' : 'odd'} aoRow">
					<td><g:checkBox name="checkOrg" value="${s.id}"
							checked="false" /></td>

					<td>
						${s.name}
					</td>

					<td>
						${s.description}
					</td>

					<td>
						${s.contact}
					</td>

					<td>
						${s.contactPhone}
					</td>

					<td>
						${s.contactEmail}
					</td>

					<td class="aoTableIcon"><g:remoteLink controller="ACGroup"
							action="_editOrg" update="orgMainType" id="${s.id}">
							<span class="glyphicon glyphicon-edit" aria-hidden="true"></span>
						</g:remoteLink> <a onclick="deleteConfirm(${s.id})" id="_deleteOnTable"
						class="orgAgMainDelete aoListIcon"> <span
							class="glyphicon glyphicon-trash" style="padding-left: 20px"
							aria-hidden="true"></span> <b>Delete</b>
					</a>
				</tr>
			</g:each>
		</tbody>
	</table>
	<script>
	$(function() {
		$("#otable").DataTable({
			"columnDefs" : [ {
				"orderable" : false,
				"targets" : [ 0, 6 ]
			} ]
		});

		


		function deleteConfirm(id){
			result = confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');
			if (result){
				jQuery.ajax({
				type:'POST', url:'/sstation/ACGroup/_deleteOnTable/'+id,
				success:function(data,textStatus){
					jQuery('#orgMainType').html(data); // this is the broken bit
					// it willdelete it but not update to show it on page
				},
				error:function(XMLHttpRequest,textStatus,errorThrown){}});
			
			} 
		}

		
	  $(document).ready(function(){
		    $("#deleteAgOnTable").mouseenter(function(){
		    	$("#deleteAgOnTable").css("cursor","pointer");
		    	   });
		        
		});

	</script>
</body>
</html>