<!DOCTYPE html>
<html>
<!-- Edit Current Student Page -->
<div id="edit-acStudent" class="content scaffold-edit table" role="main">

	<g:if test="${flash.message}">
		<div class="message" role="status">
			${flash.message}
		</div>
	</g:if>

	<g:hasErrors bean="${student}">
		<ul class="errors" role="alert">
			<g:eachError bean="${student}" var="error">
				<li
					<g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}"</g:if>><g:message
						error="${error}" /></li>
			</g:eachError>
		</ul>
	</g:hasErrors>

	<div class="mainback">
		<g:form method="PUT" controller="acStudent" id="${student.id}">
			<g:hiddenField name="version" value="${student?.version}" />
			<fieldset class="form">
				<g:render template="student/form" />
			</fieldset>

			<fieldset class="buttons">
				<div id="cd">
					<g:actionSubmit class="btn btn-default" controller ="acStudent" action="_updateStudent"
					value="Update" />
				</div>
			</fieldset>
		</g:form>
	</div>
	<!-- mainback -->

</div>
<!-- EditStudent -->
</html>
