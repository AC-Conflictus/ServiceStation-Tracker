<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Service Event Main Page</title>
</head>
<body>
	<div class="body">
		<div class="titletop">
			<h1 class="pagetitle">Please Select an Service Event to Report On</h1>
			<div class="cornerButtons"></div>
			<div class="cornerButtons"></div>
		</div>
		<div></div>
		<div id="eventMain">
			<form>
				<div class="row greyBar"></div>
				<!-- Student Table -->
				<table id="table" class="table">
					<thead>
						<tr>

							<th>Name</th>

							<th>Description</th>

							<th>Contact</th>

							<th>Contact Phone</th>

							<th>Contact Email</th>

						</tr>
					</thead>
					<tbody>
						<g:each in="${list}" status="i" var="event">
							<tr class="${(i % 2) == 0 ? 'even' : 'odd'} eventRow"
								id="${event.id}">

								
								<td id="agency">
									${event.name }
								</td>

								<td>
									${event.description }
								</td>

								<td>
									${event.contact}
								</td>

								<td>
									${event.contactPhone}
								</td>

								<td>
									${event.contactEmail}
								</td>


							</tr>
						</g:each>
					</tbody>
				</table>

			</form>
		</div>

	</div>
	<script>
		$(document).ready(function() {
			$('#table').DataTable();
		   
		    var baseLink = '<g:createLink action="eventReport" controller="reports" id="ID"/>';
		    $(".eventRow").on({
			    click: function(){
				    var idstring=$(this).attr("id");
				   
				    window.location.href = baseLink.replace("ID", idstring);
			    },
			    mouseenter:function(){
					$(this).css("cursor","pointer");
				}
			});
		});
	</script>
</body>
</html>