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
    bandanaManager.getValue(GLOBAL_CONTEXT, statskeyProp) as List<Map>
}

private saveStats(Map current) {
    String statskeyProp = "default.permissions.stats.key"
    BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
    List<Map> history = loadStats()
    if(history) {
        if(history[-1].actual != current.actual || history[-1].target != current.target){
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

private long countMissingPermissions(Map<String, List<String>> defaultPermissions, Space space) {
    space.permissions.findAll{it.groupPermission}
        .each { perm -> defaultPermissions[perm.group]?.remove(perm.type)}
    defaultPermissions.values().flatten().size()
}

defaultSpacePermissionStats(httpMethod: "GET", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body ->
    if(queryParams.containsKey("current")) {
        Map<String, List<String>>  defaultPermissions = loadDefaultPermissions()
        SpaceManager spaceMan = com.atlassian.sal.api.component.ComponentLocator.getComponent(SpaceManager.class)
        List<Space> allSpaces = spaceMan.allSpaces.findAll {!it.personal}
        long potential =  defaultPermissions.values().flatten().size() * allSpaces.size()
        long missing = allSpaces.collect { countMissingPermissions(deepCopy(defaultPermissions), it) }.sum() as Long
        Map current = [actual: potential-missing, target: potential, date : new Date()]
        if(queryParams.containsKey("save")) saveStats(current)
        return ok(new JsonBuilder([current]))
    }
    return ok("${loadStats()}")
}
