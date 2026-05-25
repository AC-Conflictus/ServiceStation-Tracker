/**
 * Controls organization and agency main page
 */
$(function(){
	 $('.aoRow').on({
			mouseenter:function(){
					$(this).find("span.glyphicon").css("color","#90062C");
					$(this).css("cursor","default")
				},
			mouseleave:function(){
					$(this).find("span.glyphicon").css("color","#FFAE00");
				}
		    });	

	    $(".aoTableIcon").find("span").on({
		    mouseenter:function(){
					$(this).css("color","#E33867");
			    },
			mouseleave:function(){
					$(this).css("color","#90062C");
				}
		    });
});