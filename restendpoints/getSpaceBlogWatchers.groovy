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


@BaseScript CustomEndpointDelegate delegate


getSpaceBlogWatchers(httpMethod: "GET", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body ->
	SpaceManager spaceMan = getComponent(SpaceManager.class)
	NotificationManager nman = getComponent(NotificationManager.class)
	
	String spaceKey = queryParams.getFirst('key')
	Space space = spaceMan.getSpace(spaceKey)
	return Response.ok(new JsonBuilder(nman.getNotificationsBySpaceAndType(space, ContentTypeEnum.BLOG).collectEntries{["${it.receiver.name}" : it.receiver.fullName]}).toString()).header("Content-Type", "application/json").build()
}