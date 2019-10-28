import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.confluence.mail.notification.NotificationManager;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.Space
import com.atlassian.confluence.search.service.ContentTypeEnum
import com.atlassian.user.GroupManager
import com.atlassian.user.Group
import com.atlassian.user.UserManager
import com.atlassian.user.User




@BaseScript CustomEndpointDelegate delegate

addBlogWatchers(httpMethod: "GET", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body ->
	SpaceManager spaceMan = getComponent(SpaceManager.class)
	GroupManager gman = getComponent(GroupManager.class)
	NotificationManager nman = getComponent(NotificationManager.class)
	UserManager uman = getComponent(UserManager.class)
	
	String spaceName = queryParams.getFirst('spaceName')
	String groupName = queryParams.getFirst('groupName')
	
	Space space = spaceMan.getSpace(spaceName)
	Group group = gman.getGroup(groupName)
	
	Set<String> spaceBlogWatchers = nman.getNotificationsBySpaceAndType(space, ContentTypeEnum.BLOG).collect {it.receiver.name}.toSet()
	
	Set<String> groupMembers = gman.getMemberNames(group).toSet()
	
	Set<String> added = groupMembers - spaceBlogWatchers
	Set<String> alreadyWatching = groupMembers - added
	
	added.each{nman.addSpaceNotification(uman.getUser(it), space, ContentTypeEnum.BLOG)}
	
	return Response.ok(new JsonBuilder([added: added, alreadyWatching : alreadyWatching]).toString()).build();
}
