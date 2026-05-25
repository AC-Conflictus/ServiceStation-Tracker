<%@ page contentType="text/html;charset=UTF-8" %>
<html>

  <div class="body">
 		 <div style="padding:10px 0px;rgb(197, 62, 62)">
		<!-- Title + Three buttons -->
		<div class="titletop" style="padding-bottom:10px">
		<h1 class="pagetitle"><%="${acStudentInstance.firstname} ${acStudentInstance.lastname}"%></h1>
		</div>
		<!-- Link to the profile or the service hour list of the current student -->
		<ul class="nav nav-tabs">
				<li class="tab2" role="presentation"><g:remoteLink controller="acStudent" params="[domain:2]" action="_studentHlist" id="${acStudentInstance.id}" update="main">Service Hour List</g:remoteLink></li>
				<li class="tab3" role="presentation"><a>Summary</a></li>
				<li class="tab4" role="presentation"><a>Connections</a></li>
 		</ul>
 		</div>
 		
 		<div id="shour">
 			<div class="table_buttons">
	<form class="navbar-form cold-md-6" role="search">
        <div class="form-group" >
          <input type="text" class="form-control" placeholder="Search">
        </div>
        <button type="submit" class="btn btn-default" style="margin-left:3px;margin-right: 30px">Submit</button>
      <div class="col-md-3 navbar-right cornerButtons" style="margin:0px"><g:remoteLink params="[domain:2]" controller="acStudent"
							action="_createShour" update="shour" id="${acStudentInstance.id}"><span style="color:#FFDE97" class="glyphicon glyphicon-plus" aria-hidden="true"></span><b > New Service Hour</b></g:remoteLink></div>
	
      </form>
      </div>
      	
  	<table class="table table-bordered" style="width:100%">
					<thead>
						<tr>
					
						<g:sortableColumn property="event" title="Event" />
					
						<g:sortableColumn property="description" title="Description" />
					
						<g:sortableColumn property="commOrg" title="Organization" />
						
						<g:sortableColumn property="duration" title="Duration"/>
						
						<g:sortableColumn property="status" title="Status"/>
						
					
					</tr>
					</thead>
					<tbody>
						<g:each in="${acStudentInstance.serviceHours}" status="i" var="s">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:remoteLink params="[domain:2]" controller="acStudent" update="shour" action="_studentShour" id="${s.id}">${s.event}</g:remoteLink></td>
					
						<td>${s.description}</td>
					
						<td>${s.commOrg}</td>
						
						<td>${s.duration}</td>
						
						<td class="status" id="${s.id}"><span class="statusValue" id="${s.status}"></span>
						<g:if test="${s.status=="Active"}">
						<span class="label label-success htableLabel" style="padding-left: 12px;
    padding-right: 12px;">${s.status}</span>
						</g:if>
						<g:elseif test="${s.status=="Pending" }">
						<span class="label label-warning htableLabel">${s.status}</span>
						</g:elseif>
						<g:elseif test="${s.status=="Reject"}">
						<span class="label label-default htableLabel" style="padding-left: 12px;
    padding-right: 12px;">${s.status}</span>
						</g:elseif>
						</td>
					
					</tr>
				</g:each>
					</tbody>
				</table>
  
  </div>
 </div>
  

</html>