<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page import="sstation.Status"%>
<%@ page import="sstation.AcStudent"%>
<html>
<!-- Create Service Hour Page -->
<div id="createS">
	<div class="formHeading greyBar">
		New Service Hour
	</div>
	<div id="create-servicehour" class="content scaffold-create"
		role="main">
		<g:if test="${flash.message}">
			<div class="message" role="status">
				${flash.message}
			</div>
		</g:if>

		<g:hasErrors bean="${shour}">
			<ul class="errors" role="alert">
				<g:eachError bean="${shour}" var="error">
					<li
						<g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}";${news}=${false}</g:if>><g:message
							error="${error}" /></li>
				</g:eachError>
			</ul>
		</g:hasErrors>

		<g:form class="mainback" controller="acStudent" method="POST"
			id="${shour.id}">

			<fieldset class="form">
				<g:render template="/shared/formShour" />
				<div
					class="fieldcontain ${hasErrors(bean: shour, field: 'status', 'error')} required">
					<label for="status"> Status: </label>
					<g:select name="status" from="${Status.values()}" />
				</div>
				<g:hiddenField name="acid" value="${acid}" />
			</fieldset>


			<div id="closeD" style="margin-top: 30px">
				<g:actionSubmit class="btn btn-default" action="saveShour" value="Create" />
			</div>
		</g:form>
		
		
	</div>



</div><!-- createS ends -->

</html>