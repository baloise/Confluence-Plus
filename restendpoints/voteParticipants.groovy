import org.apache.log4j.Level

import javax.ws.rs.core.MediaType
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript


import java.util.SortedSet
import java.util.TreeSet
import com.atlassian.confluence.core.ContentPropertyManager
import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.user.UserManager

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

import com.atlassian.confluence.core.ContentEntityObject
import com.atlassian.confluence.core.ContentEntityManager
import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.confluence.core.ContentPropertyManager
import com.atlassian.user.User
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal
import com.atlassian.confluence.core.ContentPropertyManager

@BaseScript CustomEndpointDelegate delegate

voteParticipants(httpMethod: "GET", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body ->
    voteParticipants(queryParams.collectEntries { [it.key, it.value[0]] } as Map)
}
voteParticipants(httpMethod: "POST", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body ->
    voteParticipants(new groovy.json.JsonSlurper().parseText(body) as Map)  
}


// -----------------------------------------------------------------

log.setLevel(Level.DEBUG)


Map<String,Integer> loadVotes(String votePropKey, ContentPropertyManager propMan, ContentEntityObject ce) { 
    String tmp = propMan.getTextProperty(ce, votePropKey)
    (tmp ? evaluate(tmp) : [:] ) as Map<String,Integer>
}

Response voteParticipants(Map<String,String> params){


    ContentEntityManager CEman = getComponent(ContentEntityManager.class)
    ContentEntityObject ce = CEman.getById(params.entityId as long)
    
	String currentUserName = params.username ?: AuthenticatedUserThreadLocal.get().name
	 
    ContentPropertyManager propMan = getComponent(ContentPropertyManager.class)
    String instanceId = "com-baloise-systemic-consensus-${params.subject.replaceAll(/\W/,'-')}"
    String propKey = "$instanceId-userNames"
    String tmp = propMan.getTextProperty(ce, propKey)
    SortedSet<String> userNames = (tmp?.split(',') ?: [:]) as TreeSet
    userNames.remove("")
    boolean changed = ("remove" != params.op) ? userNames.add(currentUserName) : userNames.remove(currentUserName)
    if(changed) propMan.setTextProperty(ce, propKey, userNames.join(','))

    if(params.vote) {
        String votePropKey = "$instanceId-${params.option}-votes"
        Map<String,Integer> username2vote = loadVotes(votePropKey, propMan, ce)
        changed = username2vote.put(currentUserName, params.vote as Integer) != params.vote as Integer
        if(changed)  propMan.setTextProperty(ce, votePropKey, username2vote.inspect())
    }

    List<String> options = params.options.trim().split("\r?\n") as List
    options.add("Status Quo")

    Map<String,Map<String,Integer>> option2username2vote = options.collectEntries { String option ->  
        [("$option" as String):loadVotes("$instanceId-$option-votes", propMan, ce)]
    } as Map<String, Map> 
     
    //----------------- RENDER --------------- 

    String currentToggleIcon = userNames.contains(currentUserName) ? 'aui-iconfont-remove' : 'aui-iconfont-add'

    String html = """
    <div class="panelContent">
    <div  style="position: relative;">
    <div class="buttons-container" style="position: absolute; right: 8px; top: 8px; z-index: 100;">
            <div class="buttons">
            <button class="aui-button" data-instance-id="$instanceId" name="addRemoveParticipantToVote"><span class="aui-icon aui-icon-small $currentToggleIcon"></span></button>
            <button class="aui-button" data-instance-id="$instanceId"  name="refreshVote"><span class="aui-icon aui-icon-small aui-iconfont-refresh">Refresh</span></button>
            </div>
    </div>
    </div>
    <form class="aui">
    <table class="aui">
    <thead>
            <tr>
            <th>${params.subject}</th>
            <th>${options.join("</td><td>")}</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                ${createTableBody(instanceId, userNames, options, option2username2vote)}
            </tr>
        </tbody>
    </table>
    </form>
    </div>
    """

    Response.ok(new JsonBuilder([participantCount: userNames.size(), changed : changed, html:html]).toString()).build()
}

String getSumClass(Map<String,Integer> option2sum, String option ) {
    switch(option2sum[option]) {
        case option2sum.values().max(): return "aui-lozenge-removed"
        case option2sum.values().min(): return "aui-lozenge-success"
    }
    ""
}

String createTableBody(String instanceId,SortedSet<String> userNames, List<String> options, Map<String,Map<String,Integer>> option2username2vote){
    if(!userNames) return "<td colspan=\"${options.size()+1}\"><i>no votes yet</i></td>"

    String currentUserName =  AuthenticatedUserThreadLocal.get().name
    UserManager uMan = getComponent(UserManager)
    Map<String,Integer> option2sum = [:]
    (userNames.collect{ String username -> 
       ( """<td><a class="confluence-userlink user-mention ${currentUserName == username ? 'current-user-mention' : '' }" data-username="$username" href="/atlassian/display/~$username" data-linked-resource-type="userinfo">${uMan.getUser(username)?.fullName}</a></td><td>"""+
        options.collect {  String option ->
            Integer vote = option2username2vote[option] ? option2username2vote[option][username] : null
            option2sum[option] = (option2sum[option]?:0  as int) + (vote?:0 as int)
            currentUserName == username ?
             """<aui-select
            data-instance-id="$instanceId"
            data-option="$option"
            data-voted="$vote"
            class="voteSelect"
            placeholder="How high is your resistance?"
        >""" + (0..10).collect{"<aui-option ${ it == vote ? 'selected' : '' }>$it</aui-option>"}.join('')+"</aui-select>"  :  (vote ?: '')
        }.join('</td><td>')+"</td>").toString()
    } + ['<td>&sum;</td><td>'+ options.collect {  String option -> "<span class='aui-lozenge  aui-lozenge-subtle ${getSumClass(option2sum,option)}'>${option2sum[option]}</span>"}.join('</td><td>')+'</td>'] 
    ).join('</tr><tr>') 
    
}