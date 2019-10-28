import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.confluence.mail.notification.NotificationManager
import com.atlassian.confluence.search.service.ContentTypeEnum
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.confluence.spaces.Space
import com.atlassian.user.GroupManager
import com.atlassian.user.Group
import com.atlassian.user.UserManager
import com.atlassian.user.User

@BaseScript CustomEndpointDelegate delegate


getGroupMembers(httpMethod: "GET", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body ->
	GroupManager gman = getComponent(GroupManager.class)
	UserManager uman = getComponent(UserManager.class)
	
	String groupName = queryParams.getFirst('groupName')
	Group group = gman.getGroup(groupName)
	Map users = gman.getMemberNames(group).toSet().collectEntries{
		User user = uman.getUser(it)
		["${user.name}": [email : user.email, name : user.fullName]]
	}
	return Response.ok(new JsonBuilder(users).toString()).header("Content-Type", "application/json").build()
}