<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<div class="table_buttons" style="height: 2em">
	<div class="navbar-right cornerButtons" style="margin: 0px">
		<g:link controller="hour" action="_createShour">
			<span style="color: #FFDE97" class="glyphicon glyphicon-plus"
				aria-hidden="true"></span>
			<b> New Service Hour</b>
		</g:link>
	</div>

	<g:textField name="holdingStatus" readonly="readonly" value="" />

</div>



<g:render template="/shared/hourTable" />
<script>
	
</script>
</html>