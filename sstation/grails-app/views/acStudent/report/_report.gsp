<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<!-- Main report page -->
		<!-- Title + Three buttons -->
		<div class="titletop">
		<h1 class="pagetitle">Service Hours Report</h1>
		
		<!-- Link to the profile or the service hour list of the current student -->
		<ul class="nav nav-tabs">
				<li class="tab1" role="presentation"><g:remoteLink controller="acStudent" action="_studentReport" update="report">Overall</g:remoteLink></li>
				<li class="tab2" role="presentation"><g:remoteLink controller="acStudent" action="_semesterReport" update="report">By Semester</g:remoteLink></li>
				<li class="tab3" role="presentation"><g:remoteLink controller="acStudent" action="_orgReport" update="report">Summary</g:remoteLink></li>
			
 		</ul>
 		</div><!-- Title ends -->
 		
 		<div id="report">
 		
 		</div>
</html>