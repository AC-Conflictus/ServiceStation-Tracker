<%@ page import="sstation.AcStudent"%>
<%@ page import="sstation.Status"%>
<!DOCTYPE html>
<html>
<div>
	<div class="formHeading greyBar">
		Edit Service Hour
	</div>
	<g:if test="${flash.message}">
		<div class="message" role="status">
			${flash.message}
		</div>
	</g:if>

	<g:hasErrors bean="${shour}">
		<ul class="errors" role="alert">
			<g:eachError bean="${shour}" var="error">
				<li
					<g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}"</g:if>><g:message
						error="${error}" /></li>
			</g:eachError>
		</ul>
	</g:hasErrors>

	<g:form class="mainback" id="${shour.id}" controller="acStudent"
		method="PUT">
		<fieldset class="form">
			<g:render template="/shared/formShour" />
			<g:hiddenField name="acid" value="${shour.acStudent.id}"/>
		</fieldset>

		<fieldset class="buttons">
			<div
				class="fieldcontain ${hasErrors(bean: shour, field: 'status', 'error')} required">
				<label for="status"> Status: </label>
				<g:select name="status" from="${Status.values()}"
					value="${shour.status}" />
			</div>

			<g:actionSubmit class="btn btn-default" action="saveShour"
				value="Update" />

		</fieldset>
	</g:form>
</div>
</html>
