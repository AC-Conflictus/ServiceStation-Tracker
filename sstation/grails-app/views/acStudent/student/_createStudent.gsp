<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<!-- Show a create student dialog -->
<div class="formHeading greyBar">Create A New Student</div>

<div id="createStudent" class="mainback">
	
	<g:hasErrors bean="${student}">
		<ul class="errors" role="alert">
			<g:eachError bean="${student}" var="error">
				<li>
					<g:if test="${error in org.springframework.validation.FieldError}"> data-field-id="${error.field}"</g:if><g:message
						error="${error}" /></li>
			</g:eachError>
		</ul>
	</g:hasErrors>
	
	<g:form controller="acStudent" id="${student.id}">
		<g:render template="student/form" />
		<g:actionSubmit class="btn btn-default" action="saveStudent"
			value="Save" />
		<g:actionSubmit class="btn btn-default" action="studentList"
				value="Cancel"/>
	</g:form>
</div>

</html>