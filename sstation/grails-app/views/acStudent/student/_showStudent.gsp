<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Student Profile</title>
</head>

<body>
	<!-- Student Main Page. The div "student" is used for ajax -->
	<div style="padding: 10px 0px;rgb(197, 62, 62)">
		
		<!-- Title + Three buttons -->
		<div class="titletop">
			<h1 class="pagetitle">${student.firstname} ${student.lastname}</h1>
			<div class="cornerButtons">
				<g:link controller="acStudent" action="studentList">
					<span class="glyphicon glyphicon-th-list" aria-hidden="true"></span>
					<b>Back</b>
				</g:link>

				<g:link controller="acStudent" action="deleteStudent"
					id="${student.id}">
					<span class="glyphicon glyphicon-trash" aria-hidden="true"></span>
					<b
						onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');">Delete</b>
				</g:link>
			</div>

			
			
			<!-- Link to the profile or the service hour list of the current student -->
			<ul class="nav nav-tabs">
				<li class="tab1" role="presentation" class="active"><g:link
						controller="acStudent" action="_showStudent"
						id="${student.id}">Profile</g:link></li>
				<li class="tab2" role="presentation"><g:remoteLink
						controller="acStudent" action="_hourList"
						id="${student.id}" update="student">Service Hour List</g:remoteLink></li>
				<li class="tab3" role="presentation"><g:link
						controller="acStudent" action="reportAdmin"
						id="${student.id }">Report</g:link></li>
			</ul>
		</div>


<!-- Show the details of a specific student -->
<!-- Buttons -->
 <div class="nav cornerButtons showButtons" role="navigation">
		 				<g:remoteLink controller="acStudent" action="_editStudent" id="${student.id}" update="student"><span class="glyphicon glyphicon-edit" aria-hidden="true" style="padding-left:10px"></span> <b>Edit</b></g:remoteLink>
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
  </body>
</html>