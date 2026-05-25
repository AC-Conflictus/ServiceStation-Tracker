<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Pending Service Hour Management</title>
</head>
<body>
<div>
	<!-- Buttons on the top -->
	<div class="titletop">
		<h1 class="pagetitle">Pending Service Hours Management</h1>
		<div class="cornerButtons"></div>
	</div>

	<div id="pendinghour">
		<g:render template="pendingHList" />
	</div>

</div>
</body>
</html>