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
			<div class="col-md-12" id="agTopList">

				<div class="list-group" style="width: 100%">
					<div class="list-group-item more">
						<g:link controller="commOrg" class="aoListIcon"
							action="_createAgency" update="main" params="[orgAgMain:1]">
							<span class="glyphicon glyphicon-plus" aria-hidden="true"></span> New Community Organization</g:link>
					 </div>
					 <g:each in="${list}" status="i" var="ag">

						<div class="list-group-item">
							<!-- heading -->
							<h4 class="list-group-item-heading">
								<b> ${ag.name}
								</b>
							</h4>

							<!-- Information -->
							<p class="list-group-item-text">
								${ag.description}

							</p>
							<ol class="contactInfor">
								<li>Contact: ${ag.contact}</li>
								<li>Contact Phone: ${ag.contactPhone}</li>
								<li>Contact Email: ${ag.contactEmail}</li>
								<li>Address: ${ag.address}</li>
							</ol>

							<!-- Buttons -->
							<div class="cornerButtons listButtons">
								<g:link class="aoListIcon" controller="commOrg"
									params="[orgAgMain:1]" id="${ag.id}" action="_editAgency">
									<span class="glyphicon glyphicon-edit" aria-hidden="true"></span>
									<b>Edit</b>
								</g:link>

								<a onclick="deleteConfirm(${ag.id})" id="deleteAgOnCard"
									class="orgAgMainDelete aoListIcon"> <span
									class="glyphicon glyphicon-trash" style="padding-left: 20px"
									aria-hidden="true"></span> <b>Delete</b>
								</a>
							</div>
							<!-- list-button -->

						</div>
						<!-- list-group-item -->

					</g:each>

					<div class="list-group-item more">
						<g:link controller="commOrg" action="index" class="aoListIcon">More</g:link>
					</div>

				</div>
				<!-- List-group ends -->
			</div>
			<!-- agTopList ends -->

		</div>
		<!-- row ends -->
	</div>
	<script>
	function deleteConfirm(id){
		result = confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');
		if (result){
			jQuery.ajax({
			type:'POST', url:'/sstation/commOrg/_deleteOnCard/'+id,
			success:function(data,textStatus){
				jQuery('#agMain').html(data);
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