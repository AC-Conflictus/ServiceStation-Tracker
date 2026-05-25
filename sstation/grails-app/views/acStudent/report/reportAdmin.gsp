<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Student Report</title>
</head>
<body>
	<div class="titletop">
		<h1 class="pagetitle">
			${student.firstname}
			${student.lastname}
		</h1>
		<div class="cornerButtons"></div>
	</div>
	<ul class="nav nav-tabs">
				<li class="tab1" role="presentation" class="active"><g:link
						controller="acStudent" action="student"
						id="${student.id}" >Profile</g:link></li>
				<li class="tab2" role="presentation"><g:link
						controller="acStudent" action="_hourList"
						id="${student.id}">Service Hour List</g:link></li>
				<li class="tab3" role="presentation"><g:link
						controller="acStudent" action="reportAdmin"
						id="${student.id }">Report</g:link></li>
	</ul>

	<div class="mainback" style="margin-top: 20px; margin-bottom: 20px">
		<b>Total Service Hours: </b>
		${stat.get('grandSum')}hr <br /> <b>Total Approved Service Hours: </b>
		${stat.get('aSum')}hr <br /> <b>Total Pending Service Hours: </b>
		${stat.get('pSum')}hr <br /> <b>Total Rejected Service Hours: </b>
		${stat.get('rSum')}hr
	</div>

	
	<div class="row">
		<div class="col-md-12" id="reportTopList">

			<div class="list-group">
				<div class="list-group-item">
					<h4 class="list-group-item-heading">
						<b>General Report</b>
					</h4>
					<p class="list-group-item-text">General a general report of
						service hours.</p>
					<g:link controller="acStudent" id="${student.id}"
						action="studentReport">Generate Report</g:link>
					<div class="cornerButtons listButtons"></div>
				</div>
			</div>

			<div class="list-group">
				<div class="list-group-item">
					<h4 class="list-group-item-heading">
						<b>Report by Semester</b>
					</h4>
					<p class="list-group-item-text">Generate a report of service
						hours organized by semesters</p>
					<g:link controller="acStudent" id="${student.id}"
						action="semesterReport">Generate Report</g:link>
					<div class="cornerButtons listButtons"></div>
					
				</div>
			</div>

			<div class="list-group">
				<div class="list-group-item">
					<h4 class="list-group-item-heading">
						<b>Report by Campus Group</b>
					</h4>
					<p class="list-group-item-text">Generates a report of service
						hours organized by Campus groups.</p>
					<g:link controller="acStudent" id="${student.id}"
						action="orgReport">Generate Report</g:link>
					<div class="cornerButtons listButtons"></div>
					
				</div>
			</div>

		</div>
	</div>
</body>
</html>