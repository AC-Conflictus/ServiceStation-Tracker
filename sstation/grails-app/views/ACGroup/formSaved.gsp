<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="refresh" content="0; url = index" />
<meta name="layout" content="main" />
<title>Campus Group Management</title>
</head>
<body>
	<div class="body">
	<h1>Your Campus Group Has Been Saved</h1>
		<g:if test='${table==1 }'>
			<h1>
				<g:link controller='ACGroup' action='returnToTable'>Back to Campus Groups</g:link>
			</h1>
		</g:if>
		<g:else>
			<h1>
				<g:link controller='ACGroup' action='returnToCard'>Back to Campus Groups</g:link>
			</h1>
		</g:else>
	</div>
</body>
</html>