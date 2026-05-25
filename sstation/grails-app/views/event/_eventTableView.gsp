<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>

<asset:javascript src="eventMain.js" />
<!-- not a real script yet -->
</head>
<body>
	<form>
		<div class="row greyBar">

			<div class="col-md-6 navbar-right cornerButtons"
				style="margin-top: 7px; margin-bottom: -7px;">
				<!--  <div id="createEventButton">-->
				<g:link controller="event" action="_createEvent">
					<span class="glyphicon glyphicon-plus" aria-hidden="true"></span>
					<b>New Event</b>
				</g:link>
				<span style="color: #FFDE97" class="glyphicon glyphicon-trash"
					aria-hidden="true"></span>
				<g:submitToRemote controller="event" action="deletemult"
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

					<th>Contact</th>

					<th>Contact Phone</th>

					<th>Contact Email</th>

					<th></th>

				</tr>
			</thead>
			<tbody>
				<g:each in="${list}" status="i" var="event">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'} eventRow"
						id="${event.id}">

						<td><g:checkBox name="checkEvent" value="${event.id}"
								checked="false" /></td>

						<td id="agency">
							${event.name }
						</td>

						<td>
							${event.description }
						</td>

						<td>
							${event.contact}
						</td>

						<td>
							${event.contactPhone}
						</td>

						<td>
							${event.contactEmail}
						</td>

						<td class="aoTableIcon"><g:remoteLink controller="event"
								action="_editEvent" update="eventMain" id="${event.id}">
								<span class="glyphicon glyphicon-edit" aria-hidden="true"></span>

							</g:remoteLink> <a onclick="deleteConfirm(${event.id})" id="_deleteEventTable"
							class="orgAgMainDelete aoListIcon"> <span
								class="glyphicon glyphicon-trash" aria-hidden="true"></span>

						</a></td>

					</tr>
				</g:each>
			</tbody>
		</table>

	</form>
	<script>
<
		$(function() {
			$("#table").DataTable({
				"columnDefs" : [ {
					"orderable" : false,
					"targets" : [ 0, 6 ]
				} ]
			});
		});
	
	function deleteConfirm(id){
		result = confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');
		if (result){
			jQuery.ajax({
			type:'POST', url:'/sstation/event/_deleteEventTable/'+id,
			success:function(data,textStatus){
				jQuery('#eventMain').html(data); // this is the broken bit
				// it willdelete it but not update to show it on page
			},
			error:function(XMLHttpRequest,textStatus,errorThrown){}});
		
		} 
	}
  $(document).ready(function(){
	    $("#deleteEvent").mouseenter(function(){
	    	$("#deleteEvent").css("cursor","pointer");
	    	   });
	        
	});


  </script>
</body>
</html>