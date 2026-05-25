<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Service Hour List</title>
</head>

<body>
<!-- Show a specific service hour -->
<div class="body">

	<div class="titletop">
			<h1 class="pagetitle">Service Hour</h1>
			<div class="cornerButtons">
				<g:link controller="acStudent" action="_hourList" id="${shour.acStudent.id}">
					<span class="glyphicon glyphicon-th-list" aria-hidden="true"></span>
					<b>Back</b>
				</g:link>
			</div>
	</div>
	
	<div class="nav cornerButtons showButtons" role="navigation">
		<g:link controller="acStudent" action="_editShour"
			id="${shour.id}" update="main" style="padding-right:5px">
			<span style="color: #FFDE97" class="glyphicon glyphicon-edit"
				aria-hidden="true"></span> Edit </g:link>
		<g:link controller="acStudent" action="deleteShour"
			id="${shour.id}">
			<span style="color: #FFDE97" class="glyphicon glyphicon-trash"
				aria-hidden="true"></span> Delete</g:link>

	</div>


	<!-- This place will show either the student information, the edit form, or the create form  -->
	<div class="mainback" role="main">
		<g:render template="/shared/hourInfor"/>
	</div>


</div>
</body>
</html>