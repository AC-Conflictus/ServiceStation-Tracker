
<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<!-- Log In Page -->
<!-- This page does not use main.gsp as layout template -->
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>

<!-- Latest compiled and minified JavaScript -->
<script
	src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
<link rel="stylesheet"
	href="//code.jquery.com/ui/1.11.4/themes/sunny/jquery-ui.css">
<link rel="stylesheet"
	href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css">
<!-- Optional theme -->
<link rel="stylesheet"
	href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap-theme.min.css">
	<asset:javascript src="style.js" />
<asset:stylesheet src="style.css" />
<asset:javascript src="indexForm.js"/>

<style>
body{
	background-image: url('${resource(dir: "images", file: "")}');
	background-repeat: no-repeat;
	background-size:cover;
}
.panel{
	margin: 100px 70px; 
	width: 400px; 
	padding-bottom:20px;
	box-shadow: 0 1px 2px rgba(0, 0, 0, .2);
}
.indexForm{
	margin:30px;
}
ul.nav-tabs>li{
	width:100%;

}
label.loginLabel{
	text-align:left;
}

.panelLogo{
	margin: 

}

</style>
</head>


<body>
	<div id ="container">
	

	<div id="indexDiv">
		
		<div class="panel panel-danger">
			
			<asset:image src = "ServiceStationLogo.jpg" style = "width: 400px"/>	
			
			<g:if test='${flash.message}'>
            	<div class='login_message'>${flash.message}</div>
        	</g:if>
			

			
			<!-- Log in form -->
			<form action="${createLink(uri: '/j_spring_security_check')}" method="POST" autocomplete="off" class="indexForm form-horizontal" id="logInForm">

				<!-- Email Input -->
				<div class="form-group">
					<label for="inputEmail3" class="loginLabel col-md-3 control-label"
						style="text-align: left;">AC Network ID</label>
					<div class="col-md-7">
						<input type="text" class="form-control" id="inputEmail3"
							placeholder="AC Login" name='j_username'>
					</div>
				</div>

				<!-- Password Input -->
				<div class="form-group">
					<label for="inputPassword3"
						class="loginLabel col-md-3 control-label"
						style="text-align: left;">Password</label>
					<div class="col-md-7">
						<input type="password" class="form-control" id="inputPassword3"
							placeholder="Password" name='j_password'>
					</div>
				</div>

				<!-- Remember Me 
				<div class="form-group">
					<div class="col-md-offset-3 col-md-7">
						<div class="checkbox">
							<label> <input type="checkbox" name='_spring_security_remember_me'> Remember me
							</label>
						</div>
					</div>
				</div> -->

				<!-- Log in button -->
				<div class="form-group">
					<div class="col-md-offset-3 col-md-7">
						<input type="submit" class="btn btn-default" value="Log In" />
					</div>
				</div>
			</form>	<!-- Log in Form -->
				
		</div>
</div>
</div>

			<div style="padding-left:20">
			<br/>
			<b>userid : passwd</b><br/>
			admin : admin_secret<br/>
			moderator : moderator_secret<br/>
			student: student_secret<br/>
			<br/>
			</div>

	<script type='text/javascript'>
    <!--
    (function() {
        document.forms['loginForm'].elements['j_username'].focus();
    })();
    // -->
</script>

</body>
</html>