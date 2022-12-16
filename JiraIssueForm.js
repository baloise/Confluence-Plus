 
// quirky code for IE 11

var JiraIssueForm = new JiraIssueFormLoader();
function JiraIssueFormLoader() {
	this.restUrl = '/rest/api/latest';
    this.load = function(projectId, issueTypeId) {
    	var restUrl = this.restUrl;
        return new Promise(function(resolve, reject) {
            $.ajax({
                type: "GET",
                url: restUrl+"/issue/createmeta/"+projectId+"/issuetypes/"+issueTypeId,
                contentType: "application/json; charset=utf-8",
                headers: { "X-Atlassian-Token": "no-check" },
                dataType: "json",
                cache: true,
                success: function(data){
                    resolve(new JiraIssueFormClass(projectId, issueTypeId, data, restUrl));
                },
                failure: function(errMsg) {
                    reject(Error(errMsg));
                }
            });
        });
    }
}
    
function JiraIssueFormClass(projectId, issueTypeId, data, restUrl) {
    this.data = data;
    this.restUrl = restUrl;
    this.projectId = projectId;
    this.issueTypeId = issueTypeId;
    this.uid = new Date().getTime();
    this.ifc = this;
    
    this.loadIssue = function(issueKey) {
    	var restUrl = this.restUrl;
    	return new Promise(function(resolve, reject) {
            $.ajax({
                type: "GET",
                url: restUrl+"/issue/"+issueKey,
                contentType: "application/json; charset=utf-8",
                headers: { "X-Atlassian-Token": "no-check" },
                dataType: "json",
                cache: true,
                success: function(data){
                    resolve(data);
                },
                failure: function(errMsg) {
                    reject(Error(errMsg));
                }
            });
        });
    }

    this.getField = function (fieldNameOrId) {
          return this.data.values.find(function(f){return f.fieldId == fieldNameOrId || f.name == fieldNameOrId});
    };
    
    this.getAllowedValue = function (fieldNameOrId, valueNameOrId) {
    	const ret = {};
    	ret.field = this.data.values.find(function(f){return f.fieldId == fieldNameOrId || f.name == fieldNameOrId})
    	ret.value = ret.field.allowedValues.find(function(v){return v.id == valueNameOrId || v.name == valueNameOrId})
    	return ret;
    };
    
    this.appendInput = function (html, fieldNameOrId, params) {
    	const inp = this.getInput(fieldNameOrId, params);
    	return inp ? html + inp : html;
    }
    
    this.getInput = function (fieldNameOrId, params) {
    	params = params || {};
        const field = typeof fieldNameOrId == "string" ? this.getField(fieldNameOrId) : fieldNameOrId;
        const id = field.fieldId +"_"+ this.uid;
        const description = params.description ? '<div class="description">'+params.description+'</div>' : '';
        const required = field.required ? '<span class="aui-icon icon-required" title="required" >(required)</span>' : '';
        const placeholder = params.placeholder ? ' placeholder="'+params.placeholder+'" ' : ''
        const label = params.label || field.name;
        const intro = '<div class="field-group'+(required?' required':'')+'"><label for="'+id+'">'+label+required+'</label>';
        const extro = description +'</div>';
        var inputAttribs = ' id="'+id+'" name="'+field.fieldId+'" '+ placeholder
        const type = params.type || field.schema.type
        if("textarea"== type) {
        	return intro +
        	' <textarea class="textarea medium-long-field" rows="8"'+
        		inputAttribs+'/>'+(params.template||'')+'</textarea>'
        		+ extro  
        }
        if(["number", "string", ].indexOf(type) > -1) {
            return intro +
            '<input class="text medium-long-field" type="'+type+'"'+
            inputAttribs+' value="'+(params.template||'')+'"/>'
            + extro  
        }
        if(["option", "array", "priority"].indexOf(type) > -1 && field.allowedValues) {
        	var selectTag;
        	var optionTag;
        	if(params.multiple != false && "array" == type) {
        		inputAttribs += ' multiple="" ';
        		selectTag = 'select';
        		optionTag = 'option';
        	} else {
        		selectTag = 'aui-select';
            	optionTag = 'aui-option';
            }
        	var html = intro + '<'+selectTag+' class="medium-long-field" '+ inputAttribs+'>';
        	field.allowedValues.forEach(function(allowedValue){
        		var name = allowedValue.name || allowedValue.value
        		if(!params.allowedValues || params.allowedValues.indexOf(name) > -1){
        			html+= '<'+optionTag+' value="'+allowedValue.id+'">'+name.replace(" & ", " and ")+'</'+optionTag+'>';
        		}
            }.bind(this));
            return html + '</'+selectTag+'>' + extro
        }
        console.error( "Unkown field type", type, field);
        return null;
    };
    
    this.getForm = function (params) {
    	params = params || {}
        var html = '<form class="aui" id="form_'+this.uid+'" action="#">';
        if(params.fields) {
            params.fields.forEach(function(field){
                html = this.appendInput(html, field.name, field.params);
            }.bind(this));
        } else {
        	this.data.values.forEach(function(field){
            		html = this.appendInput(html, field);
            }.bind(this));
        }
        html += 
        	'<div class="buttons-container">'+
		    '    <div class="buttons">'+
		    '        <button class="aui-button aui-button-primary" id="create_'+this.uid+'" >Create</button>'+
		    '    </div>'+
		    '</div>'+
		    '<div class="field-group" id="created_group_'+this.uid+'" style="display: none;">'+
		    '    <label >Issue created</label>'+
		    '    <div class="description" id="created_description_'+this.uid+'">'+
		    '    </div>'+
		    '</div>'+
        	'</form>'
		return html;
    }
    this.initForm = function (params) {
    	const uid = this.uid
    	const ifc = this;
    	const button = $("#create_"+uid)
    	const form = $('#form_'+uid);
		$("select[multiple]", form).auiSelect2({
			width: '350px' 
		});
    	form.serializeData = function() {
    		// lazy initialize form.fields because of aui-select 
    		if(!form.fields) form.fields =  $("input,textarea,select", form);
    		const data = {fields : {}};
    		form.fields.each(function( index, field ) {
         
    			if(!field.name) return;
    			var jfield = $(field)
				if(jfield.attr("multiple") != null) {
					var vals = jfield.find(':selected');
					if(vals.length){
					    data.fields[field.name] = []
						vals.each(function( index, value ) {
							data.fields[field.name].push({id : $(value).attr('value')});
						});
					}
					return;
				}
    			var val = $(field).val();
      	      	val = (val || '').trim() 
      	      	if(!val) return;
    			if(field.type && field.type =="number") val = parseFloat(val);
      	      	if(field.nodeName == "SELECT") {
      	      		val = {id : val}
      	      		if(ifc.getField(field.parentElement.name).schema.type == "array") {
      	      			val = [val]
      	      		}
      	      	}
    			data.fields[field.name] = val;
    		});
	        data.setField = function(fieldNameOrId, value) {
	        	data.fields[ifc.getField(fieldNameOrId).fieldId] = value;
	        }
	        data.addValue = function(fieldNameOrId, valueNameOrId) {
	        	const tmp = ifc.getAllowedValue(fieldNameOrId, valueNameOrId)
	        	data.fields[tmp.field.fieldId] = data.fields[tmp.field.fieldId] || []
	        	data.fields[tmp.field.fieldId].push({id : tmp.value.id});
	        }
	        data.getField = function(fieldNameOrId) {
	        	return data.fields[ifc.getField(fieldNameOrId).fieldId];
	        }
	        data.fields.project = {id : ifc.projectId};
	        data.fields.issuetype = {id : ifc.issueTypeId};
	        if(params.transform) params.transform(data);
	        return data;
    	}
    		
    	form.addIssue = function (url, key, summary, status , issuetype){
    		var sumShort = summary;
    		var max = 35;
    		if(sumShort.length > max) sumShort = sumShort.substr(0,max-3) + "..."

    		$("#created_group_"+uid).show();
    		$("#created_description_"+uid).append(
    		'<span class="jira-issue" data-jira-key="'+key+'" style="width:350px;"> <a'+
    		'    href="'+url+'" target="_blank"'+
    		'    class="jira-issue-key"><img class="icon"'+
    		'    title="'+issuetype.name+'" src="'+issuetype.iconUrl+'">'+key+'</a>'+
    		'    - <span class="summary" title="'+summary+'">'+sumShort+'</span> <span'+
    		'    class="aui-lozenge aui-lozenge-subtle  aui-lozenge-default     jira-macro-single-issue-export-pdf" style="float:right;">'+status+'</span>'+
    		'</span><br/>'
    		);
    	}
    	
    	form.loadAndAddIssue = function(isseKey) {
    		ifc.loadIssue(isseKey).then(function(issue) {
    			const url = form.browseLink(issue)
    			form.addIssue(url, issue.key, issue.fields.summary, issue.fields.status.name, issue.fields.issuetype)
    		},function(error) {
    			console.error(error);
    		});
    	}
    	
    	form.browseLink = function(issue) {
    		return issue.self.split("/rest/")[0]+"/browse/"+issue.key;
    	};
    	
    	button.click(function(event) {
    		var that = this;
    		event.preventDefault();
	        if (that.isBusy()){
	        	return false;
	        }
	        that.busy();
	        form.fields =  $("input,textarea,select", form);
	        form.fields.prop("disabled",true);
	        
	        AJS.$.ajax({
	            type: "POST",
	            url: "/atlassian/rest/scriptrunner/latest/custom/jira?rest/api/latest/issue",
	            data: JSON.stringify(form.serializeData()),
	            contentType: "application/json; charset=utf-8",
	            headers: { "X-Atlassian-Token": "no-check" },
	            dataType: "json",
	      		success: function(msg){
	        		that.idle();
	        		if(params.onCreate) params.onCreate(msg);
	        		if(params.listNew != false){
	        			form.loadAndAddIssue(msg.key);
	        		}
	        		if(params.flagNew != false && AJS){
	        			const url = form.browseLink(msg)
	        			AJS.flag({
	        				    type: 'success',
	        				    close : 'auto',
	        				    body: 'Issue <a class="aui-button aui-button-link" href="'+url+'" target="_blank">'+msg.key+'</a> has been created.'
	        			});
	        		}
	    			form.get(0).reset();
	    			$("select[multiple]", form).select2("val", "");
	    			form.checkEnabled();
	    			form.fields.prop("disabled",false);
	      		}
	    	});
	    	return false;
    	});
    	
    	const requiredFields = $("input,textarea,select", $("#form_"+uid+" .required"));
    	
    	form.checkEnabled = function() {
    		button.prop("disabled",!allRequired());
    	}
    	requiredFields.change(form.checkEnabled);
    	requiredFields.keyup(form.checkEnabled);  

    	function allRequired() {
    	  var c = 0;
    	  requiredFields.each(function( index, value ) {
    	      if(c > 0) return;
    		  var val = $(value).val()
    	      val = (val || '').trim() 
              if(!val) {c++;}
    	  });
    	  return c == 0; 
    	}
    	form.checkEnabled();
        form.uid = uid;
        form.button=button; 
        form.requiredFields = requiredFields;
        return form;

    }
    
    this.append = function (selector, params) {
    	params = params || {};
    	$(selector).append(this.getForm(params));
        const form = this.initForm(params);
        this.uid = new Date().getTime();
        return form;
    }
} 