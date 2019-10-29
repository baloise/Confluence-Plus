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
import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.pages.PageManager
import com.atlassian.mail.server.MailServerManager
import com.atlassian.mail.server.SMTPMailServer
import com.atlassian.mail.Email


@BaseScript CustomEndpointDelegate delegate

pageMan = getComponent(PageManager.class)
private PageManager pageMan(){pageMan as PageManager}

private Map<String, String> loadConf(){
	Page page = pageMan().getPage('About', 'Blog Manager Config')
	new XmlSlurper().parseText("<p>${page.bodyAsString}</p>").table.tbody.tr.collectEntries{[(it.th.toString().trim()) : (it.td.toString().trim())]}
}


private notify(Map<String, String> conf, User user){
	
	String message = conf.notificationMessage
							.replace('USER', user.fullName)
	
	MailServerManager mailMan = getComponent(MailServerManager.class)
	SMTPMailServer  mailServer = mailMan.defaultSMTPMailServer

	if(mailServer){
		String prefix = mailServer.prefix
		if(conf.notificationPrefix) mailServer.prefix = "[${conf.notificationPrefix}] "
		Email email = new Email(user.email)
							.setSubject(conf.notificationTitle)
							.setBody(message)
							.setReplyTo(conf.notificationReplyTo)
							.setMimeType("text/html")
		if(conf.notificationBcc) email.bcc = conf.notificationBcc
		mailServer.send(email)
		mailServer.prefix = prefix
	}
}


private doAddBlogWatchers(String spaceName, Set<String> userkeys) {
    SpaceManager spaceMan = getComponent(SpaceManager.class)
	NotificationManager nman = getComponent(NotificationManager.class)
    UserManager uman = getComponent(UserManager.class)
 
    Space space = spaceMan.getSpace(spaceName)
      
    Set<String> spaceBlogWatchers = nman.getNotificationsBySpaceAndType(space, ContentTypeEnum.BLOG).collect {it.receiver.name}.toSet()    
    
    Set<String> added = userkeys - spaceBlogWatchers
    Set<String> alreadyWatching = userkeys - added
    Set<String> notFound = new HashSet()
    
    Map<String, String> conf = loadConf()
    
    added.each{
        User user = uman.getUser(it)
        if(user) {
            nman.addSpaceNotification(user, space, ContentTypeEnum.BLOG)
        	notify(conf, user)
        } else {
            notFound.add(it)
        }
      
    }
    added = added - notFound
    
    return Response.ok(new JsonBuilder([conf : conf, space: spaceName, added: added, alreadyWatching : alreadyWatching, notFound : notFound]).toString()).header("Content-Type", "application/json").build();
    
}

addBlogWatchers(httpMethod: "GET", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body ->
    String spaceName = queryParams.getFirst('spaceName')
    if(queryParams.containsKey('user')) {
         return doAddBlogWatchers(spaceName, [queryParams.getFirst('user')].toSet())
    } 
    GroupManager gman = getComponent(GroupManager.class)
    String groupName = queryParams.getFirst('groupName')
    Group group = gman.getGroup(groupName)
    Set<String> groupMembers = gman.getMemberNames(group).toSet()
    
    return doAddBlogWatchers(spaceName, groupMembers)
    
}

addBlogWatchers(httpMethod: "POST", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body ->
    Map params = [:]
	body.split('&').each{def tmp = it.split('='); params.get(tmp[0], []).add(tmp[1])}
    return doAddBlogWatchers(params.space[0], params.keys.toSet())
}
