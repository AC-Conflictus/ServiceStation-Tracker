<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Station Report by Service Event</title>
<style>
.table>tbody>tr>td {
	border: none;
}

.table>thead>tr>th {
	background-color: transparent;
	color: black;
	border-bottom: 1px solid #ddd;
}
</style>
</head>
<body>
	<div class="mainback">

		<div class="reportTitle">Service Station Service Hours Report by
			Service Event</div>

		<div class="report"></div>

		<!-- Service Hours List -->
		<div>
			<table id="stationEventTable" class="table" style="width: 100%">
				<thead>
					<tr>
						<th>Service Event Name</th>

					</tr>
				</thead>

				<tbody>

					<g:each in="${li}" var="list">

						<tr>
							<td>
								${list.key} : ${list.value }
							</td>



						</tr>
					</g:each>
				</tbody>

			</table>
			<div id="eventDialog"></div>
		</div>
	</div>
	<script>
		$(function() {
			$("#eventDialog").dialog({
				autoOpen : false,
				resizable : true,
				height : 600,
				width : 800,
				modal : true
				
			});

			$(".eventDetail").click(function() {
				$("#eventDialog").dialog("open");
				var event = $(this).prop("id");
				$.ajax({
					type : 'GET',
					data : {
						event : event
					},
					url : '/sstation/reports/_hoursByEvent',
					success : function(data, textStatus) {
						$('#eventDialog').html(data);
					},
					error : function(XMLHttpRequest, textStatus, errorThrown) {
					}
				});
			});

		})
	</script>
</body>
</html>