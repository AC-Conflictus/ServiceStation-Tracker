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
	Service Station Service Hours Report by Year
	</div>
	
	<div class="report">
	</div>

	<!-- Service Hours List -->
	<div>
		<table id="stationYeartable" class="table" style="width: 100%">
			<thead>
				<tr>
					<th>Year</th>
					<th>Total Service Hours (hr)</th>
					<th>Average by Student (hr)</th>
					<th>Average by Service Event (hr)</th>
					<th>Average by Campus Groups (hr)</th>
					<th>Average by Community Organizations (hr)</th>
					<th></th>
				</tr>
			</thead>
			
			<tbody>
			
				<g:each in="${list}" status="i" var="year">
					<tr>
						<td>
							${year['year']}
						</td>
						<td>
							${year['total'] }
						</td>
						<td>
							${year['avgByStudent']}
						</td>

						<td>
							${year['avgByEvent']}
						</td>

						<td>
							${year['avgByGroup']}
						</td>

						<td>
							${year['avgByCommOrg']}
						</td>
						
						<td>
							<div class="btn btn-default yearDetail" id="${year['year']}">Details</div>
						</td>
					</tr>
				</g:each>
				
			</tbody>
			
		</table>
		<div id="yearDialog"></div>
	</div>
</div>
<script>
$(function(){
	 $("#yearDialog").dialog({
				autoOpen : false,
				resizable : true,
				height : 600,
				width : 800,
				modal : true,
				open: function(event, ui) { $(".ui-dialog-titlebar-close").hide(); },
				buttons : {
					Close : function() {
							$("#yearDialog").dialog("close");
						}
				}
	});

	$(".yearDetail").click(function() {
			$("#yearDialog").dialog("open");
			var year=$(this).prop("id");
			$.ajax({
				type:'GET', 
				data:{year:year},
				url:'/sstation/reports/_hoursByYear',
				success:function(data,textStatus){
					$('#yearDialog').html(data);
				},
				error:function(XMLHttpRequest,textStatus,errorThrown){}
			});
	});

	
	

})
</script>
</body>
</html>