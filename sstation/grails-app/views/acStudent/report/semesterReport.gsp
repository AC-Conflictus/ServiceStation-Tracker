<html>
<head>
<style>
.table>thead>tr>th, .table>tbody>tr>td {
	border: none;
	background-color: transparent;
	color: black;
}

.table>tbody>tr>td.term {
	border-top: 1px solid #ddd;
}
</style>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Semester Report</title>
</head>
<body>
	<div class="titletop">
		<h1 class="pagetitle">
			${student.firstname}
			${student.lastname}
		</h1>
		<div class="cornerButtons">
			<sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_MODERATOR">
				<g:link controller="acStudent" action="reportAdmin"
					id="${student.id}">Back</g:link>
			</sec:ifAnyGranted>
			<sec:ifAnyGranted roles="ROLE_STUDENT">
				<g:link controller="acStudent" action="reportStudent"
					id="${student.id}">Back</g:link>
			</sec:ifAnyGranted>
		</div>
	</div>
	<br />

	<div class="mainback">
		<div class="reportTitle">Austin College Service Hours Report</div>
		<!-- Basic Infor -->
		<div class="report">
			<b>
				${student.firstname} ${student.lastname}
			</b> <br />
			<b>Austin College ID: </b>
			${student.acid}
			<br />
			<b>Classification: </b>
			${student.classification}
		</div>

		<!-- KPI -->
		<div class="report">
			<b>Service Hours Total: </b>
			${totalSH}hr
		</div>

		<!-- Service Hours List -->
		<div>
			<table id="htable" class="table" style="border: none; width: 100%">
				<thead>

					<tr>
						<th>Service Event</th>
						<th>Campus Group</th>
						<th>Community Organization</th>
						<td>Date/Start Time</td>
						<th>Duration</th>
					</tr>
				</thead>
				<tbody>
					<g:each in="${SHlist}" status="i" var="item">
						<g:if test="${i%2==0}">
							<tr>
								<td colspan="5" class="term">
									${item}
								</td>
							</tr>
						</g:if>
						<g:else>
							<g:each in="${item}" var="s">
								<tr>
									<td>
										${s.event.name}
									
									<td>
										${s.campusOrg.name}
									</td>

									<td>
										${s.commAg.name}
									</td>

									<td>
										${s.starttime.format('MM/dd/yyyy HH:mm')}
									</td>

									<td>
										${s.duration}
									</td>


								</tr>
							</g:each>
						</g:else>
					</g:each>
				</tbody>
			</table>

		</div>
		<!-- Table End -->



	</div>
</body>
</html>