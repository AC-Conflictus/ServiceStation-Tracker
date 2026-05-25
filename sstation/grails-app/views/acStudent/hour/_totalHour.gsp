<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<div>
	<!-- Buttons on the top -->
	<div class="titletop">
		<h1 class="pagetitle">Service Hours Management</h1>
		<div class="cornerButtons"></div>
	</div>
	<div>

		<!-- Link to the profile or the service hour list of the current student -->
		<ul class="nav nav-tabs">
			<li class="tab1" role="presentation"><g:remoteLink
					controller="acStudent" action="_hourkpi" update="totalhour">Summary</g:remoteLink></li>
			<li class="tab2"><g:remoteLink controller="acStudent"
					action="_totalHList" update="totalhour">
					Table
				</g:remoteLink></li>
		</ul>
	</div>



	<div id="totalhour">
		<g:render template="kpi/hourkpi" />
	</div>

</div>

</html>