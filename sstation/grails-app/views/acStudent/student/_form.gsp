<%! import sstation.Classification %>
<meta name="layout" content="main" />
<!-- form template for student -->
<div class="fieldcontain ${hasErrors(bean: student, field: 'firstname', 'error')} required">
	<label for="firstname">
		<g:message code="acStudent.firstname.label" default="Firstname" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="firstname" required="" value="${student?.firstname}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: student, field: 'lastname', 'error')} required">
	<label for="lastname">
		<g:message code="acStudent.lastname.label" default="Lastname" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="lastname" required="" value="${student?.lastname}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: student, field: 'acid', 'error')} required">
	<label for="acid">
		<g:message code="acStudent.acid.label" default="Ac ID" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="acid" required="" value="${student?.acid}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: student, field: 'acBox', 'error')} required">
	<label for="acBox">
		<g:message code="acStudent.acBox.label" default="Ac Box" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="acBox" required="" value="${student?.acBox}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: student, field: 'acEmail', 'error')} required">
	<label for="acEmail">
		<g:message code="acStudent.acEmail.label" default="Ac Email" />
		<span class="required-indicator">*</span>
	</label>
	<g:field type="email" name="acEmail" required="" value="${student?.acEmail}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: student, field: 'classification', 'error')} required">
	<label for="classification">
		<g:message code="acStudent.classification.label" default="Classification" />
		<span class="required-indicator">*</span>
	</label>
	<g:select from="${Classification.values()}" name="classification" value="${student?.classification?:""}" required=""/>

</div>

<div class="fieldcontain ${hasErrors(bean: student, field: 'status', 'error')} required">
	<label for="status">
		<g:message code="acStudent.status.label" default="Status" />
		<span class="required-indicator">*</span>
	</label>
	<g:select from="${['R','X','W']}" name="status" value="${student?.status?:""}" required=""/>

</div>

<div class="fieldcontain ${hasErrors(bean: student, field: 'acYear', 'error')} required">
	<label for="acYear">
		<g:message code="acStudent.acYear.label" default="Ac Year" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="acYear" type="number" value="${student?.acYear?:""}" required=""/>

</div>

<div class="fieldcontain ${hasErrors(bean: student, field: 'phone', 'error')} required">
	<label for="phone">
		<g:message code="acStudent.phone.label" default="Phone" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="phone" required="" value="${student?.phone}"/>

</div>


