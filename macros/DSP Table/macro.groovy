import com.atlassian.bandana.BandanaManager
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.confluence.spaces.Space
import com.atlassian.confluence.security.SpacePermission
import static com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext.GLOBAL_CONTEXT


String png(Collection<String> permissions, String permission ){
    '/atlassian/images/icons/emoticons/' + (permissions.contains(permission.toUpperCase()) ? 'check.png' : 'error.png')
}


String ret = '''
<link type="text/css" rel="stylesheet" href="/atlassian/includes/css/space-admin.css">
<table id="gPermissionsTable" class="permissions aui" width="100%" cellspacing="0" cellpadding="2" border="0">
        
                                            <tbody><tr>
            <th width="25%">&nbsp;</th>
            <th colspan="2">All</th>
            <th colspan="2">Pages</th>
            <th colspan="2">Blog</th>
            <th colspan="2">Attachments</th>
            <th colspan="2">Comments</th>
            <th>Restrictions</th>
            <th>Mail</th>
            <th colspan="2">Space</th>
        </tr>
        <tr>
            <th>&nbsp;</th>
            <th width="40">View</th>
            <th width="40">Delete Own</th>
            <th class="permissions-group-start" width="40">Add</th>
            <th width="40">Delete</th>
            <th class="permissions-group-start" width="40">Add</th>
            <th width="40">Delete</th>
            <th class="permissions-group-start" width="40">Add</th>
            <th width="40">Delete</th>
            <th class="permissions-group-start" width="40">Add</th>
            <th width="40">Delete</th>
            <th class="permissions-group-start" width="40">Add/Delete</th>
            <th class="permissions-group-start" width="40">Delete</th>
            <th class="permissions-group-start" width="40">Export</th>
            <th width="40">Admin</th>
        </tr>'''


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
}.sort()
 .each{ group, permissions ->
    ret += """<tr class="space-permission-row">
            <td>${group}</td>
			<td class="permissionCell"   valign="middle" align="center">
                        <img src="${png(permissions, 'viewspace')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell"    valign="middle" align="center">
                        <img src="${png(permissions, 'removeowncontent')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell permissions-group-start"   valign="middle" align="center">
                        <img src="${png(permissions, 'editspace')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell"    valign="middle" align="center">
                        <img src="${png(permissions, 'removepage')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell permissions-group-start"  valign="middle" align="center">
                        <img src="${png(permissions, 'editblog')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell"    valign="middle" align="center">
                        <img src="${png(permissions, 'removeblog')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell permissions-group-start"   valign="middle" align="center">
                        <img src="${png(permissions, 'createattachment')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell"   valign="middle" align="center">
                        <img src="${png(permissions, 'removeattachment')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell permissions-group-start"   valign="middle" align="center">
                        <img src="${png(permissions, 'comment')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell"  valign="middle" align="center">
                        <img src="${png(permissions, 'removecomment')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell permissions-group-start"    valign="middle" align="center">
                        <img src="${png(permissions, 'setpagepermissions')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell permissions-group-start"  valign="middle" align="center">
                        <img src="${png(permissions, 'removemail')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell permissions-group-start"    valign="middle" align="center">
                        <img src="${png(permissions, 'exportspace')}" width="16" height="16" border="0" align="absmiddle">
            </td>
            <td class="permissionCell"  valign="middle" align="center">
                        <img src="${png(permissions, 'setspacepermissions')}" width="16" height="16" border="0" align="absmiddle">
            </td>
</tr>"""
}
return ret + '''</tbody></table>'''

