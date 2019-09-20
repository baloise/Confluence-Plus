import com.atlassian.confluence.security.PermissionManager
import org.apache.log4j.Level
import com.atlassian.bandana.BandanaManager
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.confluence.spaces.Space
import com.atlassian.confluence.security.SpacePermission
import com.atlassian.confluence.security.Permission
import static com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext.GLOBAL_CONTEXT
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal
import com.atlassian.confluence.user.ConfluenceUser
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext

private boolean isIgnored(Space space) {
    String ignoredKeyProp = "default.permissions.ignore"
    BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
    bandanaManager.getValue(new ConfluenceBandanaContext(space), ignoredKeyProp)
}



Map<String, List<String>> loadDefaultPermissions() {
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

private <T> T deepCopy(T object){
    evaluate(object.inspect()) as T
}
private long countMissingPermissions(Map<String, List<String>> defaultPermissions, Space space) {
    space.permissions.findAll{it.groupPermission}
        .each { perm -> defaultPermissions[perm.group]?.remove(perm.type)}
    defaultPermissions.values().flatten().size()
}

String ret = '<form class="aui" action="/atlassian/rest/scriptrunner/latest/custom/defaultSpacePermissionStats" method="get">'
ConfluenceUser currentUser = AuthenticatedUserThreadLocal.get()
PermissionManager pman = com.atlassian.sal.api.component.ComponentLocator.getComponent(PermissionManager.class)
SpaceManager spaceMan = com.atlassian.sal.api.component.ComponentLocator.getComponent(SpaceManager.class)
Map<String, List<String>>  defaultPermissions = loadDefaultPermissions()
long start = System.currentTimeMillis()
Map<Space, Long> mySpaces = spaceMan.allSpaces.findAll{!it.personal && pman.hasPermission(currentUser, Permission.SET_PERMISSIONS, it)}
 	.collectEntries { [(it) : countMissingPermissions(deepCopy(defaultPermissions), it)] }
mySpaces.entrySet().join('<br/>') + "<br/>took ${(System.currentTimeMillis()-start)/1000}"

mySpaces.each{space, missing ->
    if(missing == 0) {
        ret += """<fieldset class="group">
        <legend><a href="/atlassian/spaces/spacepermissions.action?key=${space.key}" style="color:Green;">${space.name}</a></legend>
        <div class="radio">
            <input class="radio" type="radio" checked="checked"
                   name="rr${space.key}" id="rr${space.key}Fix" value="rr${space.key}Fix" disabled="true"/>
            <label for="rr${space.key}Fix">Fix</label>
        </div>
    </fieldset>"""
    } else {
        String ckeckIgnored = isIgnored(space) ? 'checked="checked"' : ''
        String prefIgnored = ckeckIgnored ? 'rr' : 'ra'
        String styleIgnored = ckeckIgnored ? 'style="color:DarkOrange;"' : ''
        String ckeckRemind = ckeckIgnored ? '' : 'checked="checked"'
        String prefRemind = ckeckRemind ? 'rr' : 'ra'
        
        ret += """<fieldset class="group">
        <legend><a href="/atlassian/spaces/spacepermissions.action?key=${space.key}" style="color:Gray;">${space.name}</a></legend>
        <div class="radio">
            <input class="radio" type="radio" 
                   name="ra${space.key}" id="ra${space.key}Fix" value="ra${space.key}Fix"/>
            <label for="ra${space.key}Fix">Fix $missing</label>
        </div>
        <div class="radio">
            <input class="radio" type="radio" ${ckeckRemind}
                   name="ra${space.key}" id="${prefRemind}${space.key}Remind" value="${prefRemind}${space.key}Remind"/>
            <label for="${prefRemind}${space.key}Remind" >Remind us again</label>
        </div>
        <div class="radio">
            <input class="radio" type="radio" ${ckeckIgnored}
                   name="ra${space.key}" id="${prefIgnored}${space.key}Ignore" value="${prefIgnored}${space.key}Ignore"/>
            <label for="${prefIgnored}${space.key}Ignore" ${styleIgnored}>Stop telling the space owners</label>
        </div>
    </fieldset>"""
    } 
}

ret + '''<input type="hidden" name="myTodo" value="myTodo"><div class="buttons-container">
        <div class="buttons">
            <input class="button submit" type="submit" value="Save, make take a few seconds" id="dsp-save-button">
        </div>
    </div>
</form>'''