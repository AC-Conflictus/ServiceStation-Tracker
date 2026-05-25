
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Upload Complete</title>
</head>
<body>

	<!-- Buttons on the top -->
	<div class="titletop">
		<h1 class="pagetitle">Upload Complete</h1>
	</div>

	<!-- Link to the profile or the service hour list of the current student -->
	<div>
		${added}
		students added,
		${updated}
		students updated
	</div>
	<div>

		<g:link action="studentList" controller="AcStudent">
			<input type="button" value="back" class="button" />
		</g:link>

	</div>

</body>
</html>
