<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Station Report by Year</title>
<style>
.table>tbody>tr>td {
	border: none;
}

.table>thead>tr>th {
	background-color: transparent;
	color:black;
	border-bottom: 1px solid #ddd;
}
</style>
</head>
<body>
	<div class="mainback">
	
	<div class="reportTitle">
	Service Station Service Hours Summary Report <br>
	<h4>Total Service Hours: ${totalHours}</h4>
	</div>

	<div class="reportControls" style="margin-bottom:15px;">
		<g:form controller="reports" action="summaryReport" method="get">
			<label for="year">Year:</label>
			<g:select name="year" from="${yearList}" value="${year}" onchange="this.form.submit()" />
			<noscript><g:submitButton name="go" value="View"/></noscript>
		</g:form>
	</div>

	<div class="report">
	</div>

	<!-- Service Hours List -->
	<div>
		<table id="stationYeartable" class="table" style="width: 100%">
			<thead>
				<tr>
					<th>Campus Groups</th>
					<th>Service Events</th>
					<th>Community Organizations</th>
				
				</tr>
			</thead>
			
			<tbody>
			
				<g:each in="${(0..<constant) }" var="i">
					<tr>
						<td>
							${topOrgs.get(i).name}: ${orgHours.get(i)}
						</td>
						<td>
							${topEvs.get(i).name}: ${evHours.get(i)}
						</td>
						<td>
							${topAgs.get(i).name}: ${agHours.get(i)}
						</td>

					</tr>
				</g:each>
				
			</tbody>
			
		</table>
	
	</div>
</div>

</body>
</html>