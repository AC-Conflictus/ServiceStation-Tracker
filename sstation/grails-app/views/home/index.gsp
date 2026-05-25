<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Dashboard</title>
<style>
tr.even, tr.odd {
	font-size: 13px;
}

.kpiTab {
	padding: 7px;
	width: 200px;
	font-size: 16px;
}

.row {
	margin: 0px;
}

div.formHeading, td#homePendingMore {
	background-color: rgba(247, 243, 234, 0.85);
	text-align:center
}

div.formHeading>a,td#homePendingMore>a {
	color: #5D4F24;
}

div.col-md-7 {
	padding-right: 0px;
}

ul.nav-tabs{
	margin-bottom:20px;
}

div.homeTable {
	height: 30%
}

#homePendingTable {
	overflow: auto;
}

</style>

<script>


function showApprovalDialog(id) {

	$("#hrs_id").val(id);
	$("#modal_Status").dialog("open");
	
	
} 



$(document).ready(function() {

	$("#modal_Status").dialog({
		autoOpen : false,
		resizable : true,
		height : 300,
		width : 500,
		modal : true,
		buttons: {
			"Update Status": function() { 
				var stat = $("input[name='checkS']:checked").val();
				var hid = $("#hrs_id").val();

				$.ajax({
					  url: "${createLink(controller:'home', action:'ajaxUpdateStatus')}"+"?hid="+hid+"&stat="+stat
					}).done(function() {
					    $("#modal_Status").dialog( "close" );
					    location.reload(true);
					})

					  .fail(function() {
					    alert( "error" );
					  })
					  
					  .always(function() {
					  });

			},
			
			Cancel: function() {
			$( this ).dialog( "close" ); }
			}
	});
	
});

</script>


<title>Service Station Project</title>
</head>

<body>
	<div class="row">
	
		<div class="titletop">
			<h1 class="pagetitle">Service Station Home</h1>
			<div class="cornerButtons"></div><div class="cornerButtons">
		</div>
	
	
	<!-- Link to the profile or the service hour list of the current student -->
		<ul class="nav-tabs navGradient nav" >
				<li class="tab1"><g:link controller="event" action="index">Service Events</g:link></li>
				<li class="tab2"><g:link controller="ACGroup" action="index" >Campus Groups</g:link></li>
				<li class="tab3"><g:link controller="commOrg" action="index"><b>Community Organizations</b></g:link></li>
				<sec:ifAnyGranted roles="ROLE_ADMIN">
				<li class="tab4"><g:link controller="moderator" action="index" >Moderators</g:link></li>
				</sec:ifAnyGranted>
 		</ul>
	</div>
	
	<div class="row">
		<div id="homeCoreInfor" class="mainback">
			<b style="font-size: 15px">Core Statistics</b><br /> <br />

			<!-- Main Stats -->
			<b>Total Students: </b>
			${overallStat.get('totalSt')}
			<br> <b>Total Service Hours: </b>
			${overallStat.get('totalSH')}
			hr <br /> <b>Service Hours Per Student: </b>
			${overallStat.get('average')}
			hr <br /> <b>Service Hours in the Current Year:</b>
			${overallStat.get('totalYear')}
			hr <br /> <b>Num. Pending: </b>
			${overallStat.get('pTotal')}
			Service Hours
		</div>
		<!-- homeCoreInfor -->

		<div id="homeTable" style="margin-top: 50px; height:300px; overflow: scroll">
			<div id="homeTableName" class="formHeading">
				<g:link action="pending" controller="hour">Pending Service Hours (${list.size()})</g:link>
			</div>
			<table id="homePendingTable" class="table">
				<thead>
					<tr id="homeTableHead">
						<th>Student</th>
						<th>AC ID</th>
						<th>Service Event</th>
						<th>Community Organization</th>
						<th>Duration</th>
						<th>Status</th>
					</tr>
				</thead>
				<tbody>
					<g:each in="${list}" var="sh" status="i">
						<tr class="${i%2==0?'even':'odd'}" onclick="showApprovalDialog(${sh.id}); return false;">
							<td><b>
									${sh.acStudent.firstname} ${sh.acStudent.lastname}
							</b></td>
							<td>
								${sh.acStudent.acid}
							</td>
							<td>
								${sh.event?.name}
							</td>
							<td>
								${sh.campusOrg?.name}
							</td>
							<td>
								${sh.duration} hr
							</td>
							<td><g:if test="${sh.status.toString()=="APPROVED"}">
									<span class="label label-success htableLabel"> ${sh.status}
									</span>
								</g:if> <g:elseif test="${sh.status.toString()=="PENDING" }">
									<span class="label label-warning htableLabel"> ${sh.status}
									</span>
								</g:elseif> <g:elseif test="${sh.status.toString()=="REJECTED"}">
									<span class="label label-default htableLabel"> ${sh.status}
									</span>
								</g:elseif></td>
						</tr>
					</g:each>
				</tbody>
			</table>
		</div>
		<!-- col-md-7 ends -->

	</div>
	<!-- row -->

	<g:render template="hourkpi" />
	
	<!--  the following dialog is a quick way to allow user to change status  -->
	<div class="modal" id="modal_Status" style="display: none;">

	<h1>Status Selection</h1>
	<input id="hrs_id" type="hidden" />
	<table>
		<thead>
		</thead>
		<tbody>
			<tr>
				<td><g:radio name="checkS" value="APPROVED" checked="true" />&nbsp;APPROVED</td>
				<td><g:radio name="checkS" value="PENDING" checked="false" />&nbsp;PENDING</td>
				<td><g:radio name="checkS" value="REJECTED" checked="false" />&nbsp;REJECTED</td>
			</tr>
		</tbody>
	</table>
	
</div>
	
	<script>

	</script>

</body>
</html>