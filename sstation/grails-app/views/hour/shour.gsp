<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Service Hour Information</title>
</head>
<body>
	<!-- Show a specific service hour -->
	<div class="body">
		<!-- Title Bar -->
		<div class="titletop">
			<h1 class="pagetitle">Service Hour</h1>
			<div class="cornerButtons">
				<g:link controller="hour" action="overall">
					<span class="glyphicon glyphicon-th-list" aria-hidden="true"></span>
					<b>Back</b>
				</g:link>
			</div>
		</div>
		<div id="overallShour">
			<!-- Buttons -->

			<div class="nav cornerButtons showButtons" role="navigation">
				<g:link controller="hour" action="_editShour" id="${shour.id}"
					update="overallShour" style="padding-right:5px">
					<span style="color: #FFDE97" class="glyphicon glyphicon-edit"
						aria-hidden="true"></span> Edit </g:link>
						
				<g:link onclick="deleteConfirm(${shour.id})" >
					<span style="color: #FFDE97" class="glyphicon glyphicon-trash"
						aria-hidden="true"></span> Delete</g:link>
			</div>


			<!-- This place will show either the student information, the edit form, or the create form  -->
			<div class="content scaffold-show mainback" role="main">
				<g:render template="/shared/hourInfor" />
			</div>

		</div>
	</div>
	<script>
	function deleteConfirm(id){
		result = confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');
		if (result){
			jQuery.ajax({
			type:'POST', url:'/sstation/Hour/deleteShour/'+id,
			success:function(data,textStatus){
				jQuery('#totalhour').html(data); // this is the broken bit
				// it willdelete it but not update to show it on page
			},
			error:function(XMLHttpRequest,textStatus,errorThrown){}});
		
		} 
	}
	</script>
</body>
</html>