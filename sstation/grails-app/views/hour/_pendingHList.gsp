<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<div class="table_buttons" style="height: 2em">
			<div class="navbar-right cornerButtons" style="margin: 0px">
				<g:remoteLink controller="hour"
					action="_createShour" update="totalhour">
					<span style="color: #FFDE97" class="glyphicon glyphicon-plus"
						aria-hidden="true"></span>
					<b> New Service Hour</b>
				</g:remoteLink>
			</div>
		</div>
		
		<g:render template="/shared/hourTable"/>
		<script>
  $(document).ready(function(){
	    var table = $('table').DataTable( {
	        "order": [[ 5, "desc" ]]
	    } );

	    table.columns(7).search('pending').draw();
	   
	    var baseLink = '<g:createLink action="shour" controller="hour" id="ID"/>';
	    $(".shourRow").on({
		    click: function(){
			    var idstring=$(this).attr("id");
			   
			    window.location.href = baseLink.replace("ID", idstring);
		    },
		    mouseenter:function(){
				$(this).css("cursor","pointer");
			}
		});
	});
  </script>
</html>