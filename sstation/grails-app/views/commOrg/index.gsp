<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Community Organization Main Page</title>
</head>
<body>
	<div class="body">
		<div class="titletop">
			<h1 class="pagetitle">Community Organization Management</h1>
			<div class="cornerButtons"></div><div class="cornerButtons">
			<g:remoteLink
					controller="commOrg" action="_agCard" update="agMain">
					<span class="glyphicon glyphicon-th-large" aria-hidden="true"></span>
					Card View </g:remoteLink>
				<g:remoteLink controller="commOrg"
					action="_agTable" update="agMain">
					<span class="glyphicon glyphicon-list-alt" aria-hidden="true"></span>
					Table View </g:remoteLink>
			</div>
		</div><!-- title top ends -->		
		
		<div id="agMain">
			<g:if test="${table==1}">
			<g:render template="agTable"/>
			</g:if>
			<g:elseif test="${form==1}">
			<g:render template="agForm"/>
			</g:elseif>
			<g:else>		
			<g:render template="agCard" />
			</g:else>
		</div>

	</div>
</body>
</html>