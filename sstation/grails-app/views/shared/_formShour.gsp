<%@ page import="sstation.ServiceHour"%>
<meta name="layout" content="main" />
<!-- Form template for service hour -->

<input type="text" style="display: none" name="sid" value="${shour?.id}"></input>

<!-- The event field is optional. Students can choose the event in a drop down box. When one clicks on the option "Other"
, a text field will show up. One can write the event name in the text field -->
<div
	class="fieldcontain ${hasErrors(bean: shour, field: 'event', 'error')} required">
	<label for="event"> Event: </label>
	<g:select name="shEvent" from="${eventList}" value="${shour?.event}" />

</div>


<div
	class="fieldcontain ${hasErrors(bean: shour, field: 'description', 'error')} required">
	<label for="description"> Description: <span
		class="required-indicator">*</span>
	</label>
	<g:textField name="description" required=""
		value="${shour?.description}" />

</div>

<div
	class="fieldcontain ${hasErrors(bean: shour, field: 'campusOrg', 'error')} required">
	<label for="campusOrg"> Campus Group: </label>

	<g:hiddenField name="tableId_CamOrg" />
	<g:if test="${shour?.campusOrg}">
		<g:textField name="selected_CamOrg" readonly="readonly"
			value="${shour?.campusOrg.name}" />
	</g:if>
	<g:elseif test="${shour?.otherCamOrg}">
		<g:textField name="selected_CamOrg" readonly="readonly"
			value="${shour?.otherCamOrg}" />
	</g:elseif>
	<g:else>
		<g:textField name="selected_CamOrg" readonly="readonly" value="" />
	</g:else>
	<g:checkBox id="campusOrgOther" name="campusOrgOther" checked="false" />
	Other
</div>

<div class="modal" id="modal2_CamOrg" style="display: none;">

	<h1>Select Campus Group</h1>

	<br>

	<table id="selectTable_CamOrg" class="table">
		<thead>
			<tr>
				<th>Name</th>
				<th>Contact</th>
			</tr>
		</thead>

		<tbody>
			<g:each in="${fullOrgList?}" status="i" var="co">
				<tr>

					<td id="camOrg_Name+${i}">
						${i} <g:checkBox name="checkCO" value="${i}_${co.name}"
							checked="false" /> : ${co.name}
					</td>

					<td id="camOrg_Contact+${i}">
						${co.contact}
					</td>
				</tr>
			</g:each>
		</tbody>
	</table>

	<br>
	<p style="text-align: center">Current Selected Campus Group:</p>
	<p id="show_CamOrg" style="text-align: center"></p>
	<br> <br>
</div>


<script>
	var campusOrgFlag = true;
	$(function() {
		$("#campusOrgOther").on("click", function() {
			if ($("#campusOrgOther").prop("checked")) {
				$("input[name='selected_CamOrg']").prop("readonly", false);
				campusOrgFlag = false;
			} else {
				$("input[name='selected_CamOrg']").prop("readonly", true);
				campusOrgFlag = true;

			}
		});

		$("#selected_CamOrg").click(function() {
			if (campusOrgFlag) {
				dialog_CamOrg.dialog("open");
			}

		});

		var dialog_CamOrg = $("#modal2_CamOrg").dialog(
				{//modal_CamOrg
					autoOpen : false,
					resizable : true,
					height : 500,
					width : 800,
					modal : true,
					open : function(event, ui) {
						$(".ui-dialog-titlebar-close").hide();
					},
					buttons : {
						Select : function() {
							//window.alert("commit");

							var id = $("input[name='checkCO']:checked").val();

							//window.alert("id "+id);

							var set = id.split("_");
							var num = set[0];
							//window.alert("num "+num);
							var co = set[1];
							//window.alert("co "+co);

							$("#modal2_CamOrg").dialog("close");
							$('#selected_CamOrg').val(co);
							$("#tableId_CamOrg").val(
									$("input[name='checkCO']:checked").val());

						}
					}
				});

		//obtained from Z.C.(Ren)'s code that doesnt allow multiselection
		$("input[name='checkCO']").change(function() {
			$("input[name='checkCO']").not(this).prop('checked', false);
			var id = $("input[name='checkCO']:checked").val();
			var set = id.split("_");
			var co = set[1];
			$('#show_CamOrg').text(co);
		});

		$("#selectTable_CamOrg").DataTable();

	});
</script>

<div
	class="fieldcontain ${hasErrors(bean: shour, field: 'commAg', 'error')} required">
	<label for="commAg"> Community Organization: </label>

	<g:hiddenField name="tableId_CommAg" />

	<g:if test="${shour?.commAg}">
		<g:textField name="selected_CommAg" readonly="readonly"
			value="${shour?.commAg.name}" />
	</g:if>
	<g:elseif test="${shour?.otherCommAg}">
		<g:textField name="selected_CommAg" readonly="readonly"
			value="${shour?.otherCommAg}" />
	</g:elseif>
	<g:else>
		<g:textField name="selected_CommAg" readonly="readonly" value="" />
	</g:else>
	<g:textField name="otherName_CommAg" />


</div>

<div class="modal" id="modal2_CommAg" style="display: none;">

	<h1>Select Community Organization</h1>

	<br>

	<table id="selectTable_CommAg" class="table">
		<thead>
			<tr>
				<th>Name</th>
				<th>Contact</th>
			</tr>
		</thead>

		<tbody>
			<g:each in="${fullAgList?}" status="i" var="ca">
				<tr>

					<td id="commAg_Name+${i}">
						${i} <g:checkBox name="checkCA" value="${i}_${ca.name}"
							checked="false" /> : ${ca.name}
					</td>

					<td id="commAg_Contact+${i}">
						${ca.contact}
					</td>
				</tr>
			</g:each>
		</tbody>
	</table>

	<br>
	<p style="text-align: center">Current Selected Community
		Organization:</p>
	<p id="show_CommAg" style="text-align: center"></p>
	<br> <br>
</div>

<script>
	$(function() {
		//addded hidden textbox
		$("input[name='otherName_CommAg']").hide();

		var commAgFlag = true;
		$("#commAgOther").on("click", function() {
			if ($("#commAgOther").prop("checked")) {
				$("input[name='selected_CommAg']").prop("readonly", false);
				commAgFlag = false;
			} else {
				$("input[name='selected_CommAg']").prop("readonly", true);
				commAgFlag = true;

			}
		});

		$("#selected_CommAg").click(function() {
			if (commAgFlag) {
				dialog_CommAg.dialog("open");
			}

		});

		//tells if other is selected as comm org
		var otherCommOrg = false;

		var dialog_CommAg = $("#modal2_CommAg").dialog(
				{
					autoOpen : false,
					resizable : true,
					height : 500,
					width : 800,
					modal : true,
					open : function(event, ui) {
						$(".ui-dialog-titlebar-close").hide();
					},
					buttons : {
						Select : function() {
							//window.alert("commit");

							var id = $("input[name='checkCA']:checked").val();

							//window.alert("id "+id);

							var set = id.split("_");
							var num = set[0];
							//window.alert("num "+num);
							var co = set[1];
							//window.alert("co "+co);
							console.log("co " + co);
							$("#modal2_CommAg").dialog("close");
							$('#selected_CommAg').val(co);
							$("#tableId_CommAg").val(
									$("input[name='checkCA']:checked").val());
							if (co == "Other") {
								//opens hidden txtbox if 'other' picked
								$("input[name='otherName_CommAg']").show();
								//once a new comm org is picked then a boolean will change so that later when sending in data, the correct data cna be used
								otherCommOrg = true;
							} else {
								$("input[name='otherName_CommAg']").hide();
								otherCommOrg = false;
							}
						}
					}
				});

		$("input[name='checkCA']").change(function() {
			$("input[name='checkCA']").not(this).prop('checked', false);
			var id = $("input[name='checkCA']:checked").val();
			var set = id.split("_");
			var co = set[1];
			$('#show_CommAg').text(co);
		});

		$("#selectTable_CommAg").DataTable();

	});
</script>

<div
	class="fieldcontain ${hasErrors(bean: shour, field: 'starttime', 'error')} required">
	<label for="starttime"> Start Time: <span
		class="required-indicator">*</span>
	</label>
	<g:textField id="startdate" name="startdate" required=""
		value="${shour?.starttime?.format('MM/dd/yyyy')}" />
	<g:field type="number" name="starthour" required=""
		value="${shour?.starttime?.format('HH')?:'0'}" class="time_textField" />
	hr
	<g:field type="number" name="startmin" required=""
		value="${shour?.starttime?.format('mm')?:'0'}" class="time_textField" />
	min
</div>

<script>
	$(function() {
		$("#startdate").datepicker();
	});
</script>



<div
	class="fieldcontain ${hasErrors(bean: shour, field: 'duration', 'error')} required">
	<label for="duration"> Duration: <span
		class="required-indicator">*</span>
	</label>
	<g:textField name="duration" required="" value="${shour?.duration?:''}" />
	hr

</div>



<div
	class="fieldcontain ${hasErrors(bean: shour, field: 'eventContactName', 'error')} required">
	<label for="eventContactName"> Contact Name </label>
	<g:textField name="eventContactName" value="${shour?.eventContactName}" />

</div>

<div
	class="fieldcontain ${hasErrors(bean: shour, field: 'eventContactPhone', 'error')} required">
	<label for="eventContactPhone"> Contact Phone </label>
	<g:textField name="eventContactPhone"
		value="${shour?.eventContactPhone}" />

</div>

<div
	class="fieldcontain ${hasErrors(bean: shour, field: 'eventContactEmail', 'error')} required">
	<label for="eventContactEmail"> Contact Email </label>
	<g:field type="email" name="eventContactEmail"
		value="${shour?.eventContactEmail}" />

</div>



