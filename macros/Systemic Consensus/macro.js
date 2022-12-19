function refresh(instanceId, entityId, subject){
	try {
	
	$("#"+instanceId).find('button').each(function() {
  		this.busy()
  		$(this).disable()
	})

	$("#"+instanceId).find('input').disable()

  	} catch (error) {
  	}	
	location.reload()
}

AJS.$(document).on('click', 'button[name="refreshVote"]', function() {
	var instanceId = $(this).attr('data-instance-id')
	var instance = $("#"+instanceId)
	var entityId = instance.attr('data-entity-id')
	var subject = instance.attr('data-subject')
	
    var ajsContextPath = AJS.$('meta[name="ajs-context-path"]').attr('content')
    var that = this;
	if (!that.isBusy()) {
        that.busy()
		refresh(instanceId,entityId, subject)
    }
})

AJS.$(document).on('click', 'button[name="addRemoveParticipantToVote"]', function() {
    var instanceId = $(this).attr('data-instance-id')
	var instance = $("#"+instanceId)
	var entityId = instance.attr('data-entity-id')
	var subject = instance.attr('data-subject')
	
    var ajsContextPath = AJS.$('meta[name="ajs-context-path"]').attr('content')
    var that = this;
	if (!that.isBusy()) {
        that.busy()
        AJS.$.ajax({
		      url: ajsContextPath+"/rest/scriptrunner/latest/custom/voteParticipants",
		      type: "POST",
			  dataType: "json",
			  contentType: "application/json",
			  processData : false,
		      data: JSON.stringify ({
				  entityId : entityId,
				  subject : subject,
				  op : ($(that).children('span').hasClass( "aui-iconfont-remove" ) ? 'remove' :'add')
				  }),
		      success: function(msg){
		        if(msg.changed) {
					if($(that).children('span').hasClass( "aui-iconfont-remove" )) {
						$(that).children('span').removeClass( "aui-iconfont-remove" )
						$(that).children('span').addClass( "aui-iconfont-add" )
					} else {
						$(that).children('span').removeClass( "aui-iconfont-add" )
						$(that).children('span').addClass( "aui-iconfont-remove" )
					}
					refresh(instanceId,entityId, subject)
		        } else {
		             AJS.flag({
		                type: 'error',
						close : 'auto',
		                body: 'Something went wrong'
		            });
		        }
		      },
		      error: function(msg){
		          AJS.flag({
		          type: 'error',
		          close : 'auto',   
		          body: 'Something went wrong'
		        });
		      },
		      complete: function(jqXHR,textStatus){
		      }
		    })
    }
})