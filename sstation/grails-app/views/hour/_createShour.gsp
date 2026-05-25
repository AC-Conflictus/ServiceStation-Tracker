<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page import="sstation.Status"%>
<%@ page import="sstation.AcStudent"%>
<html>
<head>
<style>
div.panel {
	border: 1px solid rgba(150, 149, 149, 0.38);
	height: 100%;
	margin-bottom: 100px;
}

div#selectedStudent {
	height: 100%;
	padding-bottom: 100px;
}

.table>tbody>tr>td {
	border: none;
	background-color: #F7F3EA;
}

.table>thead>tr>th {
	background-color: #B20938;
}

.table {
	border: 1px solid #DAD7D0;
}

div.panel-heading {
	background-color: #B20938;
	color: white;
	font-weight: 700;
}

div.btn-group-vertical {
	width: 100%;
}
</style>
</head>
<body>
	<!-- Create Service Hour Page -->
	<g:form controller="hour" id="${shour.id}" method="POST">
		<div id="totalCreateSH" class="mainback">

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

				<fieldset class="form">
					<g:render template="/shared/formShour" />
					<div
						class="fieldcontain ${hasErrors(bean: shour, field: 'status', 'error')} required">
						<label for="status"> Status: </label>
						<g:select name="status" from="${Status.values()}" />
					</div>

					<div
						class="fieldcontain ${hasErrors(bean: shour, field: 'acStudent', 'error')} required">
						<label for="acStudent"> Student: </label> <span
							class="required-indicator">*</span>
						<g:textField name="student" required="" value=""
							readonly="readonly" />

						<g:hiddenField name="studentIdHolder" value="" />


					</div>
				</fieldset>
				<fieldset class="buttons">
					<g:actionSubmit class="btn btn-default" value="Create"
						action="saveShour" />

					<a href="javascript:location.reload(true)">Cancel</a>

				</fieldset>
			</div>
		</div>
		<!-- totalCreateSH ends -->

		<div id="Dialog">
			<g:render template="studentFilter" />
		</div>


	</g:form>

	<script>
		$(function() {
			$("#Dialog").dialog({
				dialogClass : 'no-close',
				autoOpen : false,
				resizable : true,
				height : 600,
				width : 800,
				modal : true,
				open : function(event, ui) {
					$(".ui-dialog-titlebar-close").hide();
				}
			});

			$("#totalPrevious").click(function() {
				$("#Dialog").dialog("close");
			});
			$("#student").click(function() {
				$("#Dialog").dialog("open");
			});

			$("#selectStud").click(
					function() {

						var id = $("input[name='checkstudent']:checked")
								.parents("tr").children("td.name").text();
						$("#Dialog").dialog("close");
						$('#student').val(id);
						$("#studentIdHolder").val(
								$("input[name='checkstudent']:checked").val());

					});

		});
	</script>
</body>
</html>