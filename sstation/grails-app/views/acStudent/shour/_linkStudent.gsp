<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<div id="selectS">
	<form>
		Selected: 
		<g:textField name="select"></g:textField>
		<g:select name="filter" from="${['AC ID','Name','Email','Classification','AC Year'] }"></g:select>
		<div style="height:200px">
		
		<table id="table" class="table table-striped table-bordered">
		<thead>
			<tr>
				<th style="background-color:#C53C3E;border:none">
				<input type="checkbox">
				</th>
				<th>AC ID</th>

				<th>Name</th>
				
				<th>AC Box</th>

				<th>Email</th>

				<th>Classification</th>

				<th>AC Year</th>

			</tr>
		</thead>
		<tbody>
			<g:each in="${list}" status="i" var="s">
				<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					<td>
				<g:checkBox name="checkstudent" value="${s.id}" checked="false"/>
				</td>

					<td id="studentid"><g:remoteLink action="_student" id="${s.id}" update="main">
							${s.acid}
						</g:remoteLink></td>

					<td>
						${s.firstname} ${s.lastname}
					</td>

					<td>
						${s.acBox}
					</td>

					<td>
						${s.acEmail}
					</td>

					<td>
						${s.classification}
					</td>

					<td>
						${s.acYear}
					</td>

				</tr>
			</g:each>
		</tbody>
	</table>
	</div>
	</form>
	</div>
</html>