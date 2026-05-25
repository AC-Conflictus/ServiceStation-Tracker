<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Service Hour Management</title>
</head>
<body>
<div>
	<!-- Buttons on the top -->
	<div class="titletop">
		<h1 class="pagetitle">Service Hours Management</h1>
		<div class="cornerButtons"></div>
	</div>
	<div>

		<!-- Link to the profile or the service hour list of the current student -->
		<ul class="nav nav-tabs">
			<li class="tab1"><g:remoteLink controller="hour"
					action="_totalHList" update="totalhour">
					Table
				</g:remoteLink></li>
			<li class="tab2" role="presentation"><g:remoteLink
					controller="hour" action="_hourkpi" update="totalhour">Analysis</g:remoteLink></li>
		</ul>
	</div>

	<div id="totalhour">
		<g:render template="totalHList" />
	</div>

</div>
</body>
</html>