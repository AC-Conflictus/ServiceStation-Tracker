<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>

<style>
.list-group-item-heading {
	margin-bottom: 15px;
}

.list-group-item-text {
	margin-bottom: 5px;
}

div.list-group-item:hover {
	opacity: 0.66
}

a.eventTime {
	text-decoration: underline;
	font-weight: 600;
	margin-right: 10px;
}

div.grayBar {
	background-color: rgba(33, 32, 30, 0.5);
	margin: 20px 0px 0px 0px
}
</style>
</head>
<body>
	<!-- Title -->
	<div class="row greyBar">

		<div class="col-md-6 navbar-right cornerButtons"
			style="margin-top: 7px; margin-bottom: -7px;">
			<!--  <div id="createEventButton">-->
			<g:link controller="event" action="_createEvent">
				<span class="glyphicon glyphicon-plus" aria-hidden="true"></span>
				<b>New Service Event</b>
			</g:link>
		</div>
		<!-- cornerButtons ends -->
	</div>
	<!-- row ends -->
	<!-- Student Table -->



	<!-- list -->
	<div class="list-group">

		<g:each in="${list}" status="i" var="event">

			<div class="list-group-item">
				<!-- heading -->
				<h4 class="list-group-item-heading">
					<b> ${event.name}
					</b>
				</h4>

				<!-- Information -->
				<p class="list-group-item-text">
					${event.description}
				</p>
				<ol class="contactInfor">
					<li>Contact: ${event.contact}</li>
					<li>Contact Phone: ${event.contactPhone}</li>
					<li>Contact Email: ${event.contactEmail}</li>
				</ol>
				<div id="${event.id}">
					<!--  <a class="eventTime aoListIcon">2015</a> <a class="eventTime aoListIcon">2014</a> <a
						class="eventTime aoListIcon">2013</a>
						-->
				</div>

				<!-- Buttons -->
				<div class="cornerButtons listButtons">
					<g:remoteLink class="aoListIcon" controller="event"
						params="[eventMain:1]" id="${event.id}" action="_editEvent"
						update="eventMain">
						<span class="glyphicon glyphicon-edit" aria-hidden="true"></span>
						<b>Edit</b>
					</g:remoteLink>
					<a onclick="deleteConfirm(${event.id})" id="_deleteEventCard"
						class="orgAgMainDelete aoListIcon"> <span
						class="glyphicon glyphicon-trash" style="padding-left: 20px"
						aria-hidden="true"></span> <b>Delete</b>
					</a>
				</div>
			</div>

		</g:each>
	</div>
	<!-- list-group ends -->

	<!-- Dialog that shows KPI of a specific event -->
	<div id="eventKPI"></div>



	<script>
	function deleteConfirm(id){
		result = confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');
		if (result){
			jQuery.ajax({
			type:'POST', url:'/sstation/event/_deleteEventCard/'+id,
			success:function(data,textStatus){
				jQuery('#eventMain').html(data); // this is the broken bit
				// it willdelete it but not update to show it on page
			},
			error:function(XMLHttpRequest,textStatus,errorThrown){}});
		
		} 
	}
		$(document).ready(
				function() {
					$('#table').DataTable();

					$('#eventKPI').dialog({
						model : true,
						autoOpen : false,
						width : "80%",
						height : "80%"
					});

					$(".eventTime").on(
							{
								click : function() {
									/*Enter the current student page*/
									var eventTime = $(this).html();
									var url = "_eventKPI/"
											+ $(this).parents("div:first")
													.prop("id");
									$("#eventKPI").dialog("open");
									$.ajax({
										url : url,
										data : {
											eventTime : eventTime
										},
										success : function(data, textStatus) {
											$('#eventKPI').html(data);
										},
										error : function(XMLHttpRequest,
												textStatus, errorThrown) {
										}
									});

								}
							});

				});
	</script>
</body>

</html>