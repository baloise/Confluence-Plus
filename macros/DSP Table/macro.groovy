import com.atlassian.bandana.BandanaManager
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.confluence.spaces.Space
import com.atlassian.confluence.security.SpacePermission
import static com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext.GLOBAL_CONTEXT

String label(String input) {
	[VIEWSPACE :'view space', REMOVEOWNCONTENT:'remove own content', COMMENT:'comment', EDITSPACE:'edit pages', SETSPACEPERMISSIONS:'space admin', REMOVEPAGE:'remove page',
	 REMOVECOMMENT:'remove comment', REMOVEBLOG:'remove blog', CREATEATTACHMENT:'create attachment', REMOVEATTACHMENT:'remove attachment', EDITBLOG:'edit blog',
	 EXPORTSPACE:'export space', REMOVEMAIL:'remove mail', SETPAGEPERMISSIONS:'restrict pages'][input] ?: input
}


String ret = '''
<table class="aui">
    <thead>
        <tr>
        <th id="group">Group</th>'''
com.atlassian.confluence.security.SpacePermission.GENERIC_SPACE_PERMISSIONS.each{genericPerm ->
	ret += "<th id=\"$genericPerm\">${label(genericPerm)}</th>"
}

ret += '''</tr>
    </thead>
    <tbody>'''


String spacekeyProp = "default.permissions.template.space.key"
BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
String spaceKey = bandanaManager.getValue(GLOBAL_CONTEXT, spacekeyProp)
SpaceManager spaceMan = com.atlassian.sal.api.component.ComponentLocator.getComponent(SpaceManager.class)
Space defaultPermissionsSpace = spaceMan.getSpace(spaceKey)
def defaultPermissions = defaultPermissionsSpace.getPermissions()
.findAll{it.group}
.groupBy{it.group}
.collectEntries { group, permission ->
	[group, permission.type]
}.each{ group, permissions ->
	ret += "<tr><td headers=\"group\">$group</td>"
	com.atlassian.confluence.security.SpacePermission.GENERIC_SPACE_PERMISSIONS.each{genericPerm ->
		String checked  = permissions.contains(genericPerm) ? 'checked="checked"' : ''
		String icon = """
        <label class="container">&nbsp; 
  <input type="checkbox" $checked disabled="true">
  <span class="checkmark"></span>
</label>
        """
		ret += "<td headers=\"$genericPerm\">$icon</td>"
	}
	 ret += "</tr>"
}
ret += '''</tbody>
</table>'''
return ret
