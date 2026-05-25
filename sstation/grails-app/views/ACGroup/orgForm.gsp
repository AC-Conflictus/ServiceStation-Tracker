<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<meta name="layout" content="main" />
<div class="formHeading greyBar">
	${heading}
</div>
<g:hasErrors bean="${org}">
		<ul class="errors" role="alert">
			<g:eachError bean="${org}" var="error">
				<li
					<g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}"</g:if>>Please check the ${error.field} </li>
			</g:eachError>
		</ul>
	</g:hasErrors>
	
	
<g:form class="mainback" controller="ACGroup" method="POST" id="${org.id}">
	<div
		class="fieldcontain ${hasErrors(bean: campusOrg, field: 'name', 'error')} required">
		<label for="name"> Name: <span class="required-indicator">*</span>
		</label>
		<g:textField name="name" required="" value="${org?.name}" />

	</div>
	<div
		class="fieldcontain ${hasErrors(bean: campusOrg, field: 'description', 'error')} required">
		<label for="description"> Description: <span
			class="required-indicator">*</span>
		</label>
		<g:textField name="description" required=""
			value="${org?.description}" />

	</div>
	<div
		class="fieldcontain ${hasErrors(bean: campusOrg, field: 'contact', 'error')} required">
		<label for="contact"> Contact: <span
			class="required-indicator">*</span>
		</label>
		<g:textField name="contact" required="" value="${org?.contact}" />

	</div>
	<div
		class="fieldcontain ${hasErrors(bean: campusOrg, field: 'contactPhone', 'error')} required">
		<label for="contactPhone"> Contact Phone: <span
			class="required-indicator">*</span>
		</label>
		<g:textField name="contactPhone" required=""
			value="${org?.contactPhone}" />

	</div>
	<div
		class="fieldcontain ${hasErrors(bean: campusOrg, field: 'contactEmail', 'error')} required">
		<label for="contactEmail"> Contact Email: <span
			class="required-indicator">*</span>
		</label>
		<g:textField name="contactEmail" required=""
			value="${org?.contactEmail}" />
	</div>
	
	<!-- Save Button -->
	<div>
		<g:if test="${params.orgAgMain=='1'||card==1}">
			<g:actionSubmit class="btn btn-default" action="_saveOnCard"
				value="Save"  onclick="_saveOnCard"/>
		</g:if>
		<g:else> 
			<g:actionSubmit class="btn btn-default" action="_saveOnTable" value="Save"
			onclick="_saveOnTable"/>
		</g:else>
		<g:actionSubmit class="btn btn-default" action="index"
				value="Cancel"/>
	</div>
	
	
</g:form>
</html>