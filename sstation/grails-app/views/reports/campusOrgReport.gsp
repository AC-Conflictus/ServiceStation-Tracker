<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Station Report by Community Organization</title>
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
	Service Hours by Campus Group <br>
	</div>
	
	<div class="report">
	</div>

	<!-- Service Hours List -->
	<div>
		<table id="stationYeartable" class="table" style="width: 100%">
			<thead>
				<tr>
					<th>Campus Group</th>
				
				</tr>
			</thead>
			
			<tbody>
			
				<g:each in="${(0..<orgList.size()) }" var="i">
					<tr>
						<td>
							${orgList.get(i).name}: ${hourList.get(i)}
						</td>

					</tr>
				</g:each>
				
			</tbody>
			
		</table>
	
	</div>
</div>

</body>
</html>