<%@ page contentType="text/html;charset=UTF-8"%>


<!--So what i am thinking is that, i know it might be annoying for the user
I am going to have after the switching of status for the hours, i will make it go to the hour's page that had the status change

Waht do you think?
-->
<table id="htable" class="table" style="width: 100%">
	<thead>
		<tr style="background-color: #C53C3E; border: none">
			<th><input type="checkbox" id="mainCheckBox"></th>
			<th>Student</th>
			<th>Service Event</th>
			<th>Campus Group</th>
			<th>Community Organization</th>
			<th>Start Time</th>
			<th>Duration</th>
			<th>Status</th>
		</tr>
	</thead>
	<tbody>
		<g:each in="${list}" status="i" var="s">
			<tr class="${(i % 2) == 0 ? 'even' : 'odd'} shourRow" id="${s.id}">
				<td id="tableCheckBox"><g:checkBox class="studentCB"
						name="checkStudent" value="${s.id}" checked="false"
						onclick="click2()" /></td>

				<td>
					${s.acStudent.firstname} ${s.acStudent.lastname}
				</td>
				<td>
					${s.event?.name}
				</td>

				<td>
					<g:if test ="${s.campusOrg}">
						${s.campusOrg.name}
					</g:if>
					<g:elseif test ="${s.otherCamOrg}">
						${s.otherCamOrg}
					</g:elseif>
				</td>

				<td>
					<g:if test ="${s.commAg}">
						${s.commAg.name}
					</g:if>
					<g:elseif test ="${s.otherCommAg}">
						${s.otherCommAg}
					</g:elseif>
				</td>

				<td>
					${s.starttime.format('MM/dd/yyyy HH:mm')}
				</td>

				<td>
					${s.duration}
				</td>

				
				<td class="status" id="${s.id}">
					<span class="label label-default htableLabel" onclick="clickA(this)">
						${s.status}
					</span>
				</td>
				
				<!--
				I am confused on why we need to test to see which s.status we have, 
					when no matter what the s.status will be added to the td int the span

				<td class="status" id="${s.id}"><g:if
						test="${s.status.toString()=="APPROVED"}">
						<span class="label label-success htableLabel" onclick="click1(this)">
							${s.status}
						</span>
					</g:if> <g:elseif test="${s.status.toString()=="PENDING" }"
						onclick="click1(this)">
						<span class="label label-warning htableLabel"> ${s.status}
						</span>
					</g:elseif> <g:elseif test="${s.status.toString()=="REJECTED"}"
						onclick="click1(this)">
						<span class="label label-default htableLabel"> ${s.status}
						</span>
					</g:elseif></td>
				-->

			</tr>
		</g:each>
	</tbody>
</table>

<div class="modal" id="modal_Status" style="display: none;">

	<h1>Status Selection</h1>
	<br>
	<ul>
		<li><g:checkBox name="checkS" value="APPROVED" checked="false" />&nbsp;APPROVED</li>
		<li><g:checkBox name="checkS" value="PENDING" checked="false" />&nbsp;PENDING</li>
		<li><g:checkBox name="checkS" value="REJECTED" checked="false" />&nbsp;REJECTED</li>
	</ul>
	<br>
	
	<p style="text-align: center">Selected Status</p>
	<p id="show_S" style="text-align: center"></p>
	
	<br>

	<p style="text-align: center">
		<button onclick="changeStatus()" style="text-align: center">
			Change Status</button>
	</p>
</div>

<script>
	
	var toLink = true;
	var currentStatusabel = null;
	var statusFromEle = "";

	$(document).ready(function() {

		var table = $('#htable').DataTable({
			"order" : [ [ 5, "desc" ] ]
		});

		makeDialog();

		
		//what does this do?
		$("input[name='checkS']").change(function() {
			$("input[name='checkS']").not(this).prop('checked', false);
			var i = $("input[name='checkS']:checked").val();
			$('#show_S').text(i);
			console.log("new status= "+i);
		});

	});
	
	function makeDialog(){

		$("#modal_Status").dialog({
			autoOpen : false,
			resizable : true,
			height : 500,
			width : 500,
			modal : true
		});
	}
	
	function clickA(ele){
		currentStatusLabel = ele;
		console.log("Clicking status table box");
		statusFromEle = ele.textContent();
		openStatusDialog();
	}
	
	function openStatusDialog(){
		var text = statusFromEle;
		var newInsertText = '<p id="show_S" style="text-align: center">'+text+'</p>';
		$("#show_S").replaceWith(newInsertText);
		
	}
	
	function changeStatus(){
		
	}
	
$(".htableLabel")
			.on(
					{
						click : function() {

							console.log("Clicking status");
							toLink = false;
							
							$("#modal_Status").dialog("open");
							console.log("making new current status label");
							currentStatusabel = $(this);
						}
					});
	
	//what is going on below this in "$(".shourRow").on({});"
	var baseLink = '<g:createLink action="shour" controller="hour" id="ID"/>';

	$(".shourRow").on({
		click : function() {
			console.log("clikcing row");
			var idstring = $(this).attr("id");

			if (toLink) {
				window.location.href = baseLink.replace("ID", idstring);
			}
			toLink = true;
		},
		mouseenter : function() {
			$(this).css("cursor", "pointer");
		}
	});
	/*
	var toLink = true;
	var currentStatusabel = null;

	$(document).ready(function() {

		var table = $('#htable').DataTable({
			"order" : [ [ 5, "desc" ] ]
		});

		makeDialog();

		$("input[name='checkS']").change(function() {
			$("input[name='checkS']").not(this).prop('checked', false);
			var i = $("input[name='checkS']:checked").val();
			$('#show_S').text(i);
			console.log("new status= "+i);
		});

	});

	function makeDialog(){

		$("#modal_Status").dialog({
			autoOpen : false,
			resizable : true,
			height : 500,
			width : 500,
			modal : true
		});
	}

	var baseLink = '<g:createLink action="shour" controller="hour" id="ID"/>';

	$(".shourRow").on({
		click : function() {
			console.log("clikcing row");
			var idstring = $(this).attr("id");

			if (toLink) {
				window.location.href = baseLink.replace("ID", idstring);
			}
			toLink = true;
		},
		mouseenter : function() {
			$(this).css("cursor", "pointer");
		}
	});

	$(".htableLabel")
			.on(
					{
						click : function() {

							console.log("Clicking status");
							toLink = false;
							
							$("#modal_Status").dialog("open");
							console.log("making new current status label");
							currentStatusabel = $(this);
						}
					});

	function changeStatus(){//ele
		var n = $("input[name='checkS']:checked").val();
		console.log("changing status with "+n);

		//$('#holdingStatus').val(n);
		$("#modal_Status").dialog("close");

		var num = n;
			//$("#holdingStatus").val();

		var toInput = '';
		var approved = '<span class="label label-success htableLabel" onclick="click1(this)"> APPROVED	</span>';//1
		var pending = '<span class="label label-warning htableLabel" onclick="click1(this)"> PENDING	</span>';//2
		var rejected = '<span class="label label-default htableLabel" onclick="click1(this)"> REJECTED	</span>';//3

		if (num == 1) {
			toInput = approved;
		} else if (num == 2) {
			toInput = pending;
		} else if (num == 3) {
			toInput = rejected;
		}

		currentStatusabel.replaceWith(toInput);
		//ele.replaceWith(toInput);
	
	}

	function click1(ele){
		currentStatusabel = ele;
		console.log("Clicking status");
		toLink = false;		
	}
	
	function click2() {
		console.log("Clicking checkbox");
		toLink = false;
	}
	*/
</script>
