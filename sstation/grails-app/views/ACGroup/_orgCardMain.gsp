<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<style>
.row {
	margin-top: 20px;
}

.list-group-item-heading {
	margin-bottom: 15px;
}

.list-group-item-text {
	margin-bottom: 5px;
}

.more {
	padding-bottom: 20px;
	text-align: center;
}

.more>a {
	text-decoration: underline;
	font-weight: 700;
}
</style>
</head>
<body>
	<div class="body">
		<div class="row">
			<div class="col-md-12" id="orgTopList">

				<div class="list-group">
					<div class="list-group-item more">
						<g:link class="aoListIcon" controller="ACGroup"
							action="_createOrg" update="main" params="[orgAgMain:1]">
							<span class="glyphicon glyphicon-plus" aria-hidden="true"></span> New Campus Group</g:link>
					</div>
					<g:each in="${list}" status="i" var="org">

						<div class="list-group-item">
							<!-- heading -->
							<h4 class="list-group-item-heading">
								<b> ${org.name}
								</b>
							</h4>

							<!-- Information -->
							<p class="list-group-item-text">
								${org.description}
							</p>
							<ol class="contactInfor">
								<li>Contact: ${org.contact}</li>
								<li>Contact Phone: ${org.contactPhone}</li>
								<li>Contact Email: ${org.contactEmail}</li>
							</ol>

							<!-- Buttons -->
							<div class="cornerButtons listButtons">
								<g:remoteLink class="aoListIcon" controller="ACGroup"
									id="${org.id}" action="_editOrg" update="main"
									params="[orgAgMain:1]">
									<span class="glyphicon glyphicon-edit" aria-hidden="true"></span>
									<b>Edit</b>
								</g:remoteLink>

								<a onclick="deleteConfirm(${org.id})" id="_deleteOnCard"
									class="orgAgMainDelete aoListIcon"> <span
									class="glyphicon glyphicon-trash" style="padding-left: 20px"
									aria-hidden="true"></span> <b>Delete</b>
								</a>
							</div>
							

						</div>
						<!-- list-group-item -->

					</g:each>

					<div class="list-group-item more">
						<g:link controller="ACGroup" action="orgTableView"
							class="aoListIcon">More</g:link>
					</div>

				</div>
				<!-- List-group ends -->

			</div>
			<!-- orgTopList ends -->

		</div>
		<!-- row ends -->
	</div>
	<script>
	function deleteConfirm(id){
		result = confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');
		if (result){
			jQuery.ajax({
			type:'POST', url:'/sstation/ACGroup/_deleteOnCard/'+id,
			success:function(data,textStatus){
				jQuery('#orgMainType').html(data); // this is the broken bit
				// it willdelete it but not update to show it on page
			},
			error:function(XMLHttpRequest,textStatus,errorThrown){}});
		
		} 
	}
  $(document).ready(function(){
	    $("#deleteAgOnCard").mouseenter(function(){
	    	$("#deleteAgOnCard").css("cursor","pointer");
	    	   });
	        
	});
	</script>
</body>

</html>