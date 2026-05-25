<%@ page contentType="text/html;charset=UTF-8"%>
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
			<b> ${student.firstname} ${student.lastname}
			</b> <br /> <b>Austin College ID: </b>
			${student.acid}
			<br /> <b>Classification: </b>
			${student.classification}
		</div>

		<!-- KPI -->
		<div class="report">
			<b>Total Approved Service Hours: </b>
			${totalSH}hr
		</div>
		<br />



		<!-- Service Hours List -->

		<div>
			<table id="htable" class="table table-bordered"
				style="width: 100%; border: none">
				<thead>
					<tr style="background-color: #C53C3E; border: none">
						<td>Campus Organization</td>
						<td>Event</td>
						<td>Community Agency</td>
						<td>Start Time</td>
						<td>Duration</td>
					</tr>
				</thead>
				<tbody>
					<g:each in="${listCO}" status="i" var="co">

						<g:each in="${listSH}" var="sh">
							<g:if test="${sh.campusOrg.name == co.name}">
								<tr>
									<td>
										${sh.campusOrg.name}
									</td>
									<td>
										${sh.event.name}
									</td>


									<td>
										${sh.commAg.name}
									</td>

									<td>
										${sh.starttime.format('MM/dd/yyyy HH:mm')}
									</td>

									<td>
										${sh.duration}
									</td>
								</tr>
							</g:if>
						</g:each>
					</g:each>

				</tbody>
			</table>

		</div>
	</div>
</body>
</html>