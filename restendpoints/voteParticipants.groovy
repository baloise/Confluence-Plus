import javax.ws.rs.core.MediaType
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

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
	if(queryParams.getFirst("entityId")) return  voteParticipants(queryParams.collectEntries { [it.key, it.value[0]] } as Map<String,String>)
	String currentUserName = AuthenticatedUserThreadLocal.get().name
	return Response.ok("Hello "+currentUserName, MediaType.TEXT_HTML).build()
}
voteParticipants(httpMethod: "POST", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body ->
	voteParticipants(new groovy.json.JsonSlurper().parseText(body) as Map)
}

Response voteParticipants(Map<String,String> params){
	ContentEntityManager CEman = getComponent(ContentEntityManager.class)
	ContentEntityObject ce = CEman.getById(params.entityId as long)
	
	String currentUserName = params.username ?: AuthenticatedUserThreadLocal.get().name
	 
	ContentPropertyManager propMan = getComponent(ContentPropertyManager.class)
	String instanceId = "com-baloise-systemic-consensus-${params.subject.replaceAll(/\W/,'-')}"
	String propKey = "$instanceId-userNames"
	String tmp = propMan.getTextProperty(ce, propKey)
	Set<String> uersNames = tmp ? tmp.split(',') as Set : new TreeSet()
	boolean changed = ("remove" != params.op) ? uersNames.add(currentUserName) : uersNames.remove(currentUserName)
	propMan.setTextProperty(ce, propKey, uersNames.join(','))
	return Response.ok(new JsonBuilder([participantCount: uersNames.size(), changed : changed]).toString()).build()
}