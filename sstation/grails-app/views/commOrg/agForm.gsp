<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<meta name="layout" content="main" />
<div class="formHeading greyBar">
	${heading}
</div>
<g:hasErrors bean="${agency}">
	<ul class="errors" role="alert">
		<g:eachError bean="${agency}" var="error">
			<li
				<g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}"</g:if>>Please
				check the ${error.field}
			</li>
		</g:eachError>
	</ul>
</g:hasErrors>


<g:form class="mainback" controller="commOrg" id="${agency.id}"
	method="POST">
	<div
		class="fieldcontain ${hasErrors(bean: agency, field: 'name', 'error')} required">
		<label for="name"> Name: <span class="required-indicator">*</span>
		</label>
		<g:textField name="name" required="" value="${agency?.name}" />

	</div>
	<div
		class="fieldcontain ${hasErrors(bean: agency, field: 'description', 'error')} required">
		<label for="description"> Description: <span
			class="required-indicator">*</span>
		</label>
		<g:textField name="description" required=""
			value="${agency?.description}" />

	</div>
	<div
		class="fieldcontain ${hasErrors(bean: agency, field: 'address', 'error')} required">
		<label for="address"> Address: <span
			class="required-indicator">*</span>
		</label>
		<g:textField name="address" required="" value="${agency?.address}" />

	</div>
	<div
		class="fieldcontain ${hasErrors(bean: agency, field: 'contact', 'error')} required">
		<label for="contact"> Contact: <span
			class="required-indicator">*</span>
		</label>
		<g:textField name="contact" required="" value="${agency?.contact}" />

	</div>
	<div
		class="fieldcontain ${hasErrors(bean: agency, field: 'contactPhone', 'error')} required">
		<label for="contactPhone"> Contact Phone: <span
			class="required-indicator">*</span>
		</label>
		<g:textField name="contactPhone" required=""
			value="${agency?.contactPhone}" />

	</div>
	<div
		class="fieldcontain ${hasErrors(bean: agency, field: 'contactEmail', 'error')} required">
		<label for="contactEmail"> Contact Email: <span
			class="required-indicator">*</span>
		</label>
		<g:textField name="contactEmail" required=""
			value="${agency?.contactEmail}" />

	</div>
	<div>
		<g:if test="${params.orgAgMain=='1'||card==1}">
			<g:actionSubmit class="btn btn-default" action="_saveOnCard"
				value="Save" />
		</g:if>
		<g:else>
			<g:actionSubmit class="btn btn-default" action="_saveOnTable"
				value="Save" />
		</g:else>
		<g:actionSubmit class="btn btn-default" action="index" value="Cancel" />
	</div>
</g:form>
</html>