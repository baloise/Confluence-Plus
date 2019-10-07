import com.atlassian.confluence.security.SpacePermission

import org.apache.log4j.Level
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.servlet.http.HttpServletRequest

import com.atlassian.bandana.BandanaManager
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.confluence.spaces.Space
import com.atlassian.confluence.security.SpacePermission
import static com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext.GLOBAL_CONTEXT
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext
import com.atlassian.confluence.security.PermissionManager
import com.atlassian.confluence.security.SpacePermissionManager
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal
import com.atlassian.confluence.user.ConfluenceUser
import com.atlassian.user.GroupManager
import com.atlassian.user.Group
import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.pages.PageManager
import static com.atlassian.sal.api.component.ComponentLocator.getComponent


// ---- TYPED BINDING ACCESS 

spaceMan = getComponent(SpaceManager.class)
private SpaceManager spaceMan(){spaceMan as SpaceManager}

groupMan = getComponent(GroupManager.class)
private GroupManager groupMan(){groupMan as GroupManager}

bandanaMan = getComponent(BandanaManager.class)
private BandanaManager bandanaMan(){bandanaMan as BandanaManager}

pageMan = getComponent(PageManager.class)
private PageManager pageMan(){pageMan as PageManager}

// ----


@BaseScript CustomEndpointDelegate delegate

private boolean currentUserIsAdmin(){
    getComponent(PermissionManager.class).isSystemAdministrator(AuthenticatedUserThreadLocal.get())
}

private boolean currentUserIsMember(String group){
   	GroupManager groupMan = getComponent(GroupManager.class)
   	groupMan.hasMembership(groupMan.getGroup(group), AuthenticatedUserThreadLocal.get())
}

private Response status(int code, String message) {
	Response.status(code).header("Content-Type", "text/plain").entity("$code : $message" as String).build()
}

private Response ok(String text) {
	Response.ok(text).header("Content-Type", "text/plain").build()
}

private Response ok(JsonBuilder jsonb) {
	Response.ok(jsonb.toString()).header("Content-Type", "application/json").build()
}

lazyDefaultPermissions = null
private Map<String, List<String>> loadDefaultPermissions() {
    if(!lazyDefaultPermissions){   
    Space defaultPermissionsSpace = spaceMan().getSpace(getConfigKey('space'))
    lazyDefaultPermissions = (Map<String, List<String>>) defaultPermissionsSpace.getPermissions()
    	.findAll{it.group}
        .groupBy{it.group}
    	.collectEntries { group, permission ->
    		[group, permission.type]
		}
    }
    return lazyDefaultPermissions as Map<String, List<String>>
}

private List<Map> loadStats() {
    String statskeyProp = "default.permissions.stats.key"
    (bandanaMan().getValue(GLOBAL_CONTEXT, statskeyProp) ?: []) as List<Map> 
}

private void resetStats() {
    String statskeyProp = "default.permissions.stats.key"
    bandanaMan().removeValue(GLOBAL_CONTEXT, statskeyProp)
}

private saveStats(Map current) {
    String statskeyProp = "default.permissions.stats.key"
    List<Map> history = loadStats()
    if(history) {
        if(history[-1].actual != current.actual || history[-1].target != current.target || history[-1].ignored != current.ignored){
          history.add current
          bandanaMan().setValue(GLOBAL_CONTEXT, statskeyProp, history)
        } 
    } else {
        history = [current]
        bandanaMan().setValue(GLOBAL_CONTEXT, statskeyProp, history)
    }
}

private <T> T deepCopy(T object){
    evaluate(object.inspect()) as T
}

private setIgnored(Space space, boolean ignored) {
    String ignoredKeyProp = "default.permissions.ignore"
    bandanaMan().setValue(new ConfluenceBandanaContext(space), ignoredKeyProp, ignored)
}

private boolean isIgnored(Space space) {
    String ignoredKeyProp = "default.permissions.ignore"
    bandanaMan().getValue(new ConfluenceBandanaContext(space), ignoredKeyProp)
}

private long countMissingPermissions(Map<String, List<String>> defaultPermissions, Space space) {
    space.permissions.findAll{it.groupPermission}
        .each { perm -> defaultPermissions[perm.group]?.remove(perm.type)}
    defaultPermissions.values().flatten().size()
}

private Set<String> drawOwners(Map<Space,List<String>> space2owners, Map<String,List<Space>> owner2spaces){
	Set<String> ret = []
	Set<Space> done = new TreeSet({s1,s2-> s1.key.compareTo(s2.key)}) 
	space2owners.each { space, owners -> 
		if(done.contains(space)) return
		Collections.shuffle owners
		String owner = owners.first()
		ret.add owner
		done.addAll owner2spaces[owner]
	}
	println done
	return ret
}

private Set<String> identifyOwnersOfNonCompliantSpaces() {
    Map<String, List<String>>  defaultPermissions = loadDefaultPermissions()
    Map<String,List<Space>> owner2spaces = [:]
    List<Space> nonComliantSpaces = spaceMan().allSpaces.findAll {!it.personal && !isIgnored(it) && !isCompliant(deepCopy(defaultPermissions), it)}
    Map<Space,List<String>> space2owners = nonComliantSpaces.collectEntries{ space ->
        List<String> owners = getSpaceOwners(space)
        owners.each {owner2spaces.get(it,[]).add(space)}
        [space, owners]
    }
    return drawOwners(space2owners, owner2spaces)
}

groupUserCache = [:]
private List<String> getUsers(String groupName) {
    if(!groupName) return ['admin']
    if(!groupUserCache[groupName]) {
        Group group =  groupMan().getGroup(groupName)
        groupUserCache[groupName] = group ? groupMan().getMemberNames(group).collect{it} : ['admin']
    }
    groupUserCache[groupName]
}

private List<String> getSpaceOwners(Space space) {
    space.permissions.findAll{it.type == 'SETSPACEPERMISSIONS'}.collect{
        it.userPermission ? it.userName : getUsers(it.group)
    }.flatten().unique() as List<String>
}
private boolean isCompliant(Map<String, List<String>> defaultPermissions, Space space) {
    countMissingPermissions(defaultPermissions, space) == 0
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

private Map getCurrent(String user = null) {
    long start = System.currentTimeMillis()
    Map<String, List<String>>  defaultPermissions = loadDefaultPermissions()
    List<Space> allSpaces = spaceMan().allSpaces.findAll {!it.personal}
    List<Space> ignoredSpaces = allSpaces.findAll {isIgnored(it)}
    
    long ignored   = defaultPermissions.values().flatten().size() * ignoredSpaces.size()
    allSpaces = allSpaces - ignoredSpaces
    long potential = defaultPermissions.values().flatten().size() * allSpaces.size()
    long missing = allSpaces.collect { countMissingPermissions(deepCopy(defaultPermissions), it) }.sum() as Long
    return [actual: potential-missing, target: potential,ignored: ignored, date : new Date(), user:user, time : (System.currentTimeMillis()- start) ]
}
defaultSpacePermissionStats(httpMethod: "GET") { MultivaluedMap queryParams, String body, HttpServletRequest request ->
 (request.remoteHost != '127.0.0.1' && !currentUserIsMember('confluence-users')) ? status(403, 'permission denied') : defaultSpacePermissionStatsDo(queryParams, body)
}

private Map configDefaults(){['space' : 'About', 'page' : 'Confluence Default Space Permissions Config']}
private String getConfigKey(String config){
    def v = bandanaMan().getValue(GLOBAL_CONTEXT, "default.permissions.template.${config}.key")
    return v ? v.toString() : configDefaults()[config] 
}
conf = null
private Map loadConf(){
    if(!conf) {
		Page page = pageMan().getPage(getConfigKey('space'), getConfigKey('page'))
		conf = new XmlSlurper().parseText("<p>${page.bodyAsString}</p>").table.tbody.tr.collectEntries{[(it.th.toString().trim()) : (it.td.toString().trim())]}
    }
	conf as Map
}

private Response defaultSpacePermissionStatsDo(MultivaluedMap queryParams, String body){
    if(queryParams.containsKey("config")) {
		String config = queryParams.getFirst('config')
		String defaultValue = configDefaults()[config]
		
        String newValue = queryParams.getFirst("newValue")
       	if(defaultValue && queryParams.containsKey("reset")){
            newValue = defaultValue
       	}
        if(defaultValue && newValue){
          	bandanaMan().setValue(GLOBAL_CONTEXT, "default.permissions.template.${config}.key", newValue)
         	return ok(newValue)       
       	}
        if(defaultValue) {
            return ok(getConfigKey(config))
        } else {
        	return ok(new JsonBuilder(loadConf())) 
        }
	}
    if(queryParams.containsKey("myTodo")) {
        Map<String, List<String>>  defaultPermissions = loadDefaultPermissions()
        
        
        def actions = queryParams.values().flatten().findAll{(it as String).startsWith('ra')}
        actions.each {
            String action = (it as String)[2..-1]
            if(action.endsWith('Fix')) {
                action = action[0..-4]
                Space space = spaceMan().getSpace(action)
                setIgnored(space, false)
                fixMissingPermissions(deepCopy(defaultPermissions), space)
            } else if(action.endsWith('Ignore')){ 
                action = action[0..-7]
                Space space = spaceMan().getSpace(action)
                setIgnored(space, true)
            } else if(action.endsWith('Remind')){ 
                action = action[0..-7]
                Space space = spaceMan().getSpace(action)
                setIgnored(space, false)
            }
        }
        if(actions) saveStats(getCurrent(AuthenticatedUserThreadLocal.get().fullName))
        
        
		def props = ScriptRunnerImpl.getOsgiService(ApplicationProperties)
        String spaceKey = getConfigKey('space')
        String pageKey =java.net.URLEncoder.encode(loadConf().page)
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
    if(queryParams.containsKey("find")) {
        return ok(new JsonBuilder(identifyOwnersOfNonCompliantSpaces()))
    }
    return ok(new JsonBuilder(loadStats()))
}
