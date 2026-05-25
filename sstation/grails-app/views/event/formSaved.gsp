<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="refresh" content="0; url = index" />
<meta name="layout" content="main" />
<title>Event Management</title>
</head>
<body>
	<div class="body">
	<h1>Your Event Has Been Saved</h1>
		<g:if test='${table==1 }'>
			<h1>
				<g:link controller='Event' action='returnToTable'>Back to Event page</g:link>
			</h1>
		</g:if>
		<g:else>
			<h1>
				<g:link controller='Event' action='returnToCard'>Back to Event page</g:link>
			</h1>
		</g:else>
	</div>
</body>
</html>