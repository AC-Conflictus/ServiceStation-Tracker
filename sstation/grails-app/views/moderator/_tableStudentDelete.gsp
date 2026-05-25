<form>

	<!-- Student Table -->
	<table id="tableS" class="table">
		<thead>
			<tr>
				<th>Selected</th>
					
				<th>AC ID</th>

				<th>Name</th>

				<th>AC Box</th>

				<th>Email</th>

				<th>Classification</th>

				<th>AC Year</th>

			</tr>
		</thead>
		<tbody>
			<g:each in="${list}" status="i" var="s">
				<tr class="${(i % 2) == 0 ? 'even' : 'odd'} studentRow" id="${s.id}">
					
					<td>
					<g:if test="${s.isModerator}">
					<g:checkBox id="s${s.id}" name="checkstudent" value="${s.id}"
							checked="true" disabled="true"/>
					</g:if>
					<g:else>
					<g:checkBox id="s${s.id}" name="checkstudent" value="${s.id}"
							checked="false" disabled="true" />
					</g:else>
					<script>
						$('#s${s.id}').prop('disabled', true);
					</script>
					</td>
					
		
					
					<td class="acid"><a 
					onclick="
						$('#s${s.id}').prop('checked', false);
						jQuery.ajax({
						type:'POST', url:'/sstation/moderator/_deleteModerator/${s.id}',
						success:function(data,textStatus){
						jQuery('#slist').html(data);},
						error:function(XMLHttpRequest,textStatus,errorThrown){}
						});
						return false;" id="1">
						${s.acid }
						</a>	</td>
				

					<td>
						${s.firstname} ${s.lastname}
					</td>

					<td>
						${s.acBox}
					</td>

					<td>
						${s.acEmail}
					</td>

					<td>
						${s.classification}
					</td>

					<td>
						${s.acYear}
					</td>

				</tr>
			</g:each>
		</tbody>
	</table>

	<div class="row col-md-offset-6">
		<div class="btn btn-default" id="deselectMode">Close</div>

	</div>

</form>
<script>
	$(document).ready(
			function() {
				
				$('#tableS').DataTable();

				$("#deselectMode").click(function() {
					$("#tableStudent").dialog("close");

				});
				$(".acid").mouseenter(function(){
			    	$(".acid").css("cursor","pointer");
			    	   });
				
			

			});
</script>