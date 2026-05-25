/**
 * Controls the logic on the index page
 */
$(function(){
	$("#signUpLink").click(function(){
		$("#signUpForm").show();
		$("#logInForm").hide();
	});
	$("#logInLink").click(function(){
		$("#logInForm").show();
		$("#signUpForm").hide();
	});
});