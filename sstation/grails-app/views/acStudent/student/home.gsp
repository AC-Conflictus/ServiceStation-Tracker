
<!-- This page shows the basic information of the current selected studnet -->
<!DOCTYPE html>
<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Student Management</title>
</head>

<body>
	<!-- Student Main Page. The div "student" is used for ajax -->
	<div style="padding: 10px 0px;rgb(197, 62, 62)">
		
		<!-- Title + Three buttons -->
		<div class="titletop">
			<h1 class="pagetitle">${student.firstname} ${student.lastname}</h1>			
			
			<!-- Link to the profile or the service hour list of the current student -->
				<!-- Link to the profile or the service hour list of the current student -->
			<ul class="nav nav-tabs">
				<li class="tab1" role="presentation" class="active"><g:link
						controller="acStudent" action="home"
						id="${student.id}" >Profile</g:link></li>
				<li class="tab2" role="presentation"><g:link
						controller="acStudent" action="_hourList"
						id="${student.id}">Service Hour List</g:link></li>
				<li class="tab3" role="presentation"><g:link
						controller="acStudent" action="reportStudent"
						id="${student.id }">Report</g:link></li>
			</ul>
		</div>


		<!-- This place will show either the student information, the edit form, or the create form  -->
		<div id="student">
			<!-- This place will show either the student information, the edit form, or the create form  -->
		<div id="student">
			<div class="nav cornerButtons showButtons" role="navigation">
		 				<g:link controller="acStudent" action="_editStudent" id="${student.id}"><span class="glyphicon glyphicon-edit" aria-hidden="true" style="padding-left:10px"></span> <b>Edit</b></g:link>
</div>

<!-- Information -->
<div id="show-acStudent" class="content scaffold-show mainback" role="main">
			<ol class="property-list acStudent">
			
				<g:if test="${student?.firstname}">
				<li class="fieldcontain">
					<b>First Name:</b>  ${student.firstname}	
				</li>
				</g:if>
		
				<g:if test="${student?.lastname}">
				<li class="fieldcontain">
					<b>Last Name:</b>  ${student.lastname}					
				</li>
				</g:if>
			
				<g:if test="${student?.acid}">
				<li class="fieldcontain">
					<b>AC ID:</b>  ${student.acid}					
				</li>
				</g:if>
				
				<g:if test="${student?.classification}">
				<li class="fieldcontain">
					<b>Classification:</b>  ${student.classification}					
				</li>
				</g:if>
				
				<g:if test="${student?.status}">
				<li class="fieldcontain">
					<b>Status:</b>  ${student.status}					
				</li>
				</g:if>
			
				<g:if test="${student?.acYear}">
				<li class="fieldcontain">
					<b>AC Year:</b>  ${student.acYear}
				</li>
				</g:if>
			
				<g:if test="${student?.acBox}">
				<li class="fieldcontain">
					<b>AC Box:</b>  ${student.acBox}					
				</li>
				</g:if>
			
				<g:if test="${student?.acEmail}">
				<li class="fieldcontain">
					<b>AC Email:</b>  ${student.acEmail}
					
				</li>
				</g:if>
			
			
				<g:if test="${student?.phone}">
				<li class="fieldcontain">
					<b>Phone:</b>  ${student.phone}
					
				</li>
				</g:if>
			
			</ol>
			</div>
  </div>
		</div>
	</div>
</body>
</html>
