import com.atlassian.confluence.security.SpacePermission

import org.apache.log4j.Level
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

import com.atlassian.bandana.BandanaManager
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.confluence.spaces.Space
import com.atlassian.confluence.security.SpacePermission
import static com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext.GLOBAL_CONTEXT
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext
import com.atlassian.confluence.security.SpacePermissionManager
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.sal.api.ApplicationProperties


@BaseScript CustomEndpointDelegate delegate

private Response status(int code, String message) {
	Response.status(code).header("Content-Type", "text/plain").entity("$code : $message" as String).build()
}

private Response ok(String text) {
	Response.ok(text).header("Content-Type", "text/plain").build()
}

private Response ok(JsonBuilder jsonb) {
	Response.ok(jsonb.toString()).header("Content-Type", "application/json").build()
}

private Map<String, List<String>> loadDefaultPermissions() {
    String spacekeyProp = "default.permissions.template.space.key"
    BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
    String spaceKey = bandanaManager.getValue(GLOBAL_CONTEXT, spacekeyProp)
    SpaceManager spaceMan = com.atlassian.sal.api.component.ComponentLocator.getComponent(SpaceManager.class)
    Space defaultPermissionsSpace = spaceMan.getSpace(spaceKey)
    (Map<String, List<String>>) defaultPermissionsSpace.getPermissions()
    	.findAll{it.group}
        .groupBy{it.group}
    	.collectEntries { group, permission ->
    		[group, permission.type]
		} 
}

private List<Map> loadStats() {
    String statskeyProp = "default.permissions.stats.key"
    BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
    (bandanaManager.getValue(GLOBAL_CONTEXT, statskeyProp) ?: []) as List<Map> 
}

private void resetStats() {
    String statskeyProp = "default.permissions.stats.key"
    BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
    bandanaManager.removeValue(GLOBAL_CONTEXT, statskeyProp)
}

private saveStats(Map current) {
    String statskeyProp = "default.permissions.stats.key"
    BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
    List<Map> history = loadStats()
    if(history) {
        if(history[-1].actual != current.actual || history[-1].target != current.target || history[-1].ignored != current.ignored){
          history.add current
          bandanaManager.setValue(GLOBAL_CONTEXT, statskeyProp, history)
        } 
    } else {
        history = [current]
        bandanaManager.setValue(GLOBAL_CONTEXT, statskeyProp, history)
    }
}

private <T> T deepCopy(T object){
    evaluate(object.inspect()) as T
}

private setIgnored(Space space, boolean ignored) {
    String ignoredKeyProp = "default.permissions.ignore"
    BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
    bandanaManager.setValue(new ConfluenceBandanaContext(space), ignoredKeyProp, ignored)
}

private boolean isIgnored(Space space) {
    String ignoredKeyProp = "default.permissions.ignore"
    BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
    bandanaManager.getValue(new ConfluenceBandanaContext(space), ignoredKeyProp)
}

private long countMissingPermissions(Map<String, List<String>> defaultPermissions, Space space) {
    space.permissions.findAll{it.groupPermission}
        .each { perm -> defaultPermissions[perm.group]?.remove(perm.type)}
    defaultPermissions.values().flatten().size()
}

private fixMissingPermissions(Map<String, List<String>> defaultPermissions, Space space) {
    SpacePermissionManager spMan = com.atlassian.sal.api.component.ComponentLocator.getComponent(SpacePermissionManager.class)
    space.permissions.findAll{it.groupPermission}
        .each { perm -> defaultPermissions[perm.group]?.remove(perm.type)}
    defaultPermissions.each{group, permissions -> 
        permissions.each { permission -> 
            spMan.savePermission(SpacePermission.createGroupSpacePermission(permission, space, group))
        }
    }
}

private Map getCurrent() {
    Map<String, List<String>>  defaultPermissions = loadDefaultPermissions()
    SpaceManager spaceMan = com.atlassian.sal.api.component.ComponentLocator.getComponent(SpaceManager.class)
    List<Space> allSpaces = spaceMan.allSpaces.findAll {!it.personal}
    long ignored   = defaultPermissions.values().flatten().size() * allSpaces.findAll {isIgnored(it)}.size()
    allSpaces = allSpaces.findAll {!isIgnored(it)}
    long potential = defaultPermissions.values().flatten().size() * allSpaces.size()
    long missing = allSpaces.collect { countMissingPermissions(deepCopy(defaultPermissions), it) }.sum() as Long
    return [actual: potential-missing, target: potential,ignored: ignored, date : new Date()]
}

defaultSpacePermissionStats(httpMethod: "GET", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body ->
    if(queryParams.containsKey("myTodo")) {
        SpaceManager spaceMan = com.atlassian.sal.api.component.ComponentLocator.getComponent(SpaceManager.class)
        Map<String, List<String>>  defaultPermissions = loadDefaultPermissions()
        int log = 0
        queryParams.values().flatten().findAll{(it as String).startsWith('ra')}.each {
            String action = (it as String)[2..-1]
            if(action.endsWith('Fix')) {
                action = action[0..-4]
                Space space = spaceMan.getSpace(action)
                setIgnored(space, false)
                fixMissingPermissions(deepCopy(defaultPermissions), space)
                log ++
            } else if(action.endsWith('Ignore')){ 
                action = action[0..-7]
                Space space = spaceMan.getSpace(action)
                setIgnored(space, true)
                log ++
            } else if(action.endsWith('Remind')){ 
                action = action[0..-7]
                Space space = spaceMan.getSpace(action)
                setIgnored(space, false)
                log ++
            }
        }
        if(log) saveStats(getCurrent())
        
		def props = ScriptRunnerImpl.getOsgiService(ApplicationProperties)
        String spacekeyProp = "default.permissions.template.space.key"
        String pageKeyProp = "default.permissions.template.page.key"
        BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
        String spaceKey = bandanaManager.getValue(GLOBAL_CONTEXT, spacekeyProp)
        String pageKey = bandanaManager.getValue(GLOBAL_CONTEXT, pageKeyProp)?: "Confluence+Default+Space+Permissions"
        return Response.temporaryRedirect(URI.create("$props.baseUrl/display/$spaceKey/$pageKey")).build()
        
    }
    if(queryParams.containsKey("reset")) {
        resetStats()
        return ok("stats reset")
    }
    if(queryParams.containsKey("current")) {
        Map current = getCurrent()
        if(queryParams.containsKey("save")) saveStats(current)
        return ok(new JsonBuilder([current]))
    }
    return ok(new JsonBuilder(loadStats()))
}
