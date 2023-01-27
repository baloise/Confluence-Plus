var ajsContextPath = AJS.$('meta[name="ajs-context-path"]').attr('content')

function update(instanceId, html) {
	AJS.$('#'+instanceId).html(html)
}

function refresh(instanceId, entityId, subject,options){
	try {
	
	$("#"+instanceId).find('button').each(function() {
  		this.busy()
  		$(this).disable()
	})

	$("#"+instanceId).find('input').disable()

  	} catch (error) {
  	}	
	AJS.$.ajax({
		      url: ajsContextPath+"/rest/scriptrunner/latest/custom/voteParticipants",
		      type: "POST",
			  dataType: "json",
			  contentType: "application/json",
			  processData : false,
		      data: JSON.stringify ({
				  entityId : entityId,
				  subject : subject,
				  options : options
          }),
		      success: function(msg){
				update(instanceId, msg.html)   
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

AJS.$(document).on('click', 'button[name="refreshVote"]', function() {
	var instanceId = $(this).attr('data-instance-id')
	var instance = $("#"+instanceId)
	var entityId = instance.attr('data-entity-id')
	var subject = instance.attr('data-subject')
	var options = instance.attr('data-options').replaceAll("\\n", "\n")
	
    var that = this;
	if (!that.isBusy()) {
        that.busy()
		refresh(instanceId,entityId, subject,options)
    }
})

AJS.$(document).on('click', 'button[name="help"]', function() {
	window.open("https://en.wikipedia.org/wiki/Systemic_Consensing#Method", "Wikipedia - Systemic Consensing")
})


AJS.$(document).on('change', '.voteSelect', function() {
	var instanceId = $(this).attr('data-instance-id')
	var instance = $("#"+instanceId)
	var entityId = instance.attr('data-entity-id')
	var subject = instance.attr('data-subject')
	var options = instance.attr('data-options').replaceAll("\\n", "\n")

    var that = this;
	if($(that).attr('data-voted') != $(that).find(":selected").val()) {
	
	    AJS.$.ajax({
		      url: ajsContextPath+"/rest/scriptrunner/latest/custom/voteParticipants",
		      type: "POST",
			  dataType: "json",
			  contentType: "application/json",
			  processData : false,
		      data: JSON.stringify ({
				  entityId : entityId,
				  subject : subject,
				  options : options,
				  option : $(that).attr('data-option'),
				  vote : $(that).find(":selected").val()
				  }),
		      success: function(msg){
				update(instanceId, msg.html) 
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

AJS.$(document).on('click', 'button[name="addRemoveParticipantToVote"]', function() {
    var instanceId = $(this).attr('data-instance-id')
	var instance = $("#"+instanceId)
	var entityId = instance.attr('data-entity-id')
	var subject = instance.attr('data-subject')
	var options = instance.attr('data-options').replaceAll("\\n", "\n")

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
				  options : options,
				  op : ($(that).children('span').hasClass( "aui-iconfont-remove" ) ? 'remove' :'add')
				  }),
		      success: function(msg){
		        if(!msg.changed) {
					AJS.flag({
		                type: 'error',
						close : 'auto',
		                body: 'Something went wrong'
		            });
		        }
				update(instanceId, msg.html) 
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