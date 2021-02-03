function initLikeMacro(macroId, liking){
	
	function AJSMeta(prop){
	    return AJS.$('meta[name="ajs-'+prop+'"]').attr("content");
	}
	
	var button = $("#"+macroId+"b");
	var counter = $("#"+macroId+"c");
	button.get(0).liking = liking;
	button.click(function(){
	 	  var that = this;
	    if (!that.isBusy()) {
	        that.busy();
	        updateLike(that);
	    }
	});
	
	function setCounter(c){
	        counter.html(c);
	        counter.css("cursor", c ? "pointer" : "auto");
	}
	
	counter.click(function(){
	 if(counter.css("cursor") == "auto") return;
	 var f = new AJS.Dialog(400, 365, "likes-dialog");
	  AJS.$.ajax({
	      url: AJSMeta("context-path")+"/rest/scriptrunner/latest/custom/like",
	      type: "GET",
	      data: ({pageId : AJSMeta("page-id"), macroId :macroId}),
	      headers : {'X-Atlassian-Token' : "nocheck"},
	  	  dataType: "html",
		  cache : false,
	      success: function(msg){
	       	 f.getCurrentPanel().html(msg);
	         setCounter($(msg).find("li").length);
	    },
	      error : function(msg){
	        console.error(msg);
	        f.hide();
	        AJS.flag({
	            type: 'error',
	            body: msg,
	            close: 'auto',
	        });
	      }
	  });
	  f.addHeader("People who like this");
	  f.addPanel("Panel 1", "<div class='spinner-container'><aui-spinner size='medium'></aui-spinner></div>");
	  f.addCancel("Close", function(a) {
			f.hide();
	  });
	  f.getCurrentPanel().setPadding(0);
	  f.show();
	});
	function updateLike(that) {
	AJS.$.ajax({
	      url: AJSMeta("context-path")+"/rest/scriptrunner/latest/custom/"+(button.get(0).liking?"unlike":"like"),
	      type: "POST",
	      data: ({pageId : AJSMeta("page-id"), macroId :macroId}),
	      headers : {'X-Atlassian-Token' : "nocheck"},
	  		dataType: "json",
	      success: function(msg){
	        var wasLiking = button.get(0).liking; 
	        button.get(0).liking = !wasLiking;
	        button.find("span").html(wasLiking? " Like" : " Unlike");
	        setCounter(msg.length);
	        that.idle();
	    },
	      error : function(msg){
	        console.error(msg);
	        that.idle();
	        AJS.flag({
	            type: 'error',
	            body: msg,
	            close: 'auto',
	        });
	      }
	  });
	}
}