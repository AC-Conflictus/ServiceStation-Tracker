/**
 * This javascript file supports the stylesheet style.css
 */
$(function(){
	/*
	 * Navigation bar tabs states change
	 */
	$("ul.navGradient li").on({
		click:function() {
			//alert("hello");		
			/*$("ul.navGradient li").each(function(){
			$(this).removeClass("activeTab");
		});*/
			/*$(this).addClass("activeTab");*/
		}
	/*mouseenter: function(){
			alert("hello");		
			$("ul.navGradient li").each(function(){
				$(this).removeClass("activeTab");
			});
			$(this).addClass("activeTab");
	    }*/
	});

//	$("ul.navLink li").click(function() {	
//		$("ul.navLink li").each(function(){
//			$(this).removeClass("activePill");
//		});
//		$(this).addClass("activePill");
//	});

	/*Methods about tables*/
	$("tr.odd").on({
		mouseenter: function(){
			$(this).css({"background-color": "rgba(255, 255, 255, 0.4)","cursor":"pointer","color":"black"});
		},
		mouseleave: function(){
			$(this).css({"background-color":"rgba(251, 247, 205, 0.79)","color":"#545353"});
		}
	});
	$("tr.even").on({
		mouseenter: function(){
			$(this).css({"background-color": "rgba(255,255,255,0.4)","cursor":"pointer","color":"black"});
		},
		mouseleave:function(){
			$(this).css({"background-color":"rgba(255,255,255,0.8)","color":"#545353"});
		}
	});

	$(".list-group-item").on({
		mouseenter: function(){
			$(this).css({"background-color": "rgba(255, 255, 255, 0.6)","color":"black"});
			$(this).find("a").css("color","#90062C");
		},
		mouseleave:function(){
			$(this).css({"background-color":"rgba(247, 243, 234, 0.85);","color":"#545353"});
			$(this).find("a").css("color","#FFAE00");
		}
	});
	
	 $(".aoListIcon").on({
		    mouseenter:function(){
					$(this).css("color","#E33867");
			    },
			mouseleave:function(){
					$(this).css("color","#90062C");
				}
		    });
	
	
});