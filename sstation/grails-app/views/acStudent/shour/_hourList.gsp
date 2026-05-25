<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Student Management</title>
</head>

<body>
	<!-- Student Main Page. The div "student" is used for ajax -->
	<div style="padding: 10px 0px;rgb(197, 62, 62)">
		
		<!-- Title + Three buttons -->
		<div class="titletop">
			<h1 class="pagetitle">${student.firstname} ${student.lastname}</h1>
			<div class="cornerButtons">
			<sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_MODERATOR">
				<g:link controller="acStudent" action="studentList">
					<span class="glyphicon glyphicon-th-list" aria-hidden="true"></span>
					<b>Back</b>
				</g:link>

				<g:link controller="acStudent" action="deleteStudent"
					id="${student.id}">
					<span class="glyphicon glyphicon-trash" aria-hidden="true"></span>
					<b
						onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');">Delete</b>
				</g:link>
				</sec:ifAnyGranted>
			</div>

			
			
			<!-- Link to the profile or the service hour list of the current student -->
			<ul class="nav nav-tabs">
				<sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_MODERATOR">
				<li class="tab1" role="presentation" class="active"><g:link
						controller="acStudent" action="student"
						id="${student.id}" >Profile</g:link></li>
				</sec:ifAnyGranted>
				<sec:ifAnyGranted roles="ROLE_STUDENT">
				<li class="tab1" role="presentation" class="active"><g:link
						controller="acStudent" action="home"
						id="${student.id}" >Profile</g:link></li>
				</sec:ifAnyGranted>
				
				<li class="tab2" role="presentation"><g:link
						controller="acStudent" action="_hourList"
						id="${student.id}">Service Hour List</g:link></li>
				
				<sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_MODERATOR">
				<li class="tab3" role="presentation"><g:link
						controller="acStudent" action="reportAdmin"
						id="${student.id }">Report</g:link></li>
				</sec:ifAnyGranted>
				<sec:ifAnyGranted roles="ROLE_STUDENT">
					<li class="tab3" role="presentation"><g:link
						controller="acStudent" action="reportStudent"
						id="${student.id }">Report</g:link></li>
				</sec:ifAnyGranted>
			</ul>
		</div>

	<!-- Buttons -->
	<div class="table_buttons" style="height: 2em">
		<div class="col-md-3 navbar-right cornerButtons" style="margin: 0px;">
			<g:link controller="acStudent" action="_createShour"
				update="main" id="${student.id}">
				<span style="color: #FFDE97" class="glyphicon glyphicon-plus"
					aria-hidden="true"></span>
				<b> New Service Hour</b>
			</g:link>
		</div>
	</div>
	
	<!-- KPI -->
	<div class="mainback">
		<b>Total Service Hours: </b>
		${stat.get('grandSum')}hr <br /> <b>Total Approved Service Hours: </b>
		${stat.get('aSum')}hr <br /> <b>Total Pending Service Hours: </b>
		${stat.get('pSum')}hr <br /> <b>Total Rejected Service Hours: </b>
		${stat.get('rSum')}hr
	</div>


	<!-- Service Hour Table -->
	<g:render template="/shared/hourTable" />



	<!-- Status Dialog -->
	<div id="statusD">
		<form>
			<div class="form-group">
				<g:hiddenField name="shid" id="shid" />
				<select class="form-control" name="statusChange" id="statusChange">
					<option class="editStatus">APPROVED</option>
					<option class="editStatus">PENDING</option>
					<option class="editStatus">REJECTED</option>
				</select>
			</div>
			<div id="submitStatus">
				<g:submitToRemote value="Update" controller="acStudent"
					action="updateStatus" update="student" />
			</div>
		</form>
	</div>
</div>
<script>
	$(function() {

		$("#htable").DataTable();

		var baseLink = '<g:createLink action="shour" controller="acStudent" id="ID"/>';
		$(".shourRow").on({
			click : function() {
				var idstring = $(this).attr("id");

				window.location.href = baseLink.replace("ID", idstring);
			},
			mouseenter : function() {
				$(this).css("cursor", "pointer");
			}
		});

		$("#statusD").dialog({
			model : true,
			autoOpen : false
		});
		$(".status").on({
			click : function() {
				$("#statusD").dialog("open");
				var getshid = $(this).prop("id");
				$("#shid").val(getshid);

				var text = $(this).children("span.statusValue").prop("id");
				if (text == "Approved") {
					$("#statusChange").prop("selectedIndex", 0);
				} else if (text == "Pending") {
					$("#statusChange").prop("selectedIndex", 1);
				} else if (text == "Reject") {
					$("#statusChange").prop("selectedIndex", 2);
				}
			},
			mouseenter : function() {
				$(this).css("cursor", "pointer");
			}

		});

		$("#submitStatus").click(function() {
			$("#statusD").dialog("close");
		});

	});
</script>
</html>