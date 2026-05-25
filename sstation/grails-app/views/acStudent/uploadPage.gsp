
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="layout" content="main" />
<title>Student Upload</title>
</head>
<body>

	<!-- Buttons on the top -->
	<div class="titletop">
		<h1 class="pagetitle">Student Upload</h1>
	</div>

	<!-- Link to the profile or the service hour list of the current student -->
	<div>

		<g:uploadForm name="uploadCSV" action="upload" controller="AcStudent">
			<input type="file" name="CSV" />
			<g:submitButton name="UploadButton" value="Upload" />
		</g:uploadForm>

	</div>

</body>
</html>
