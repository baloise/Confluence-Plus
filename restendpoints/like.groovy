import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.servlet.http.HttpServletRequest

import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.confluence.core.ContentPropertyManager
import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.pages.PageManager
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal
import com.atlassian.confluence.user.ConfluenceUser
import com.atlassian.confluence.user.UserAccessor
import com.atlassian.user.UserManager

@BaseScript CustomEndpointDelegate delegate


private Response resp(entity) {
	entity == null ? Response.status(400).header("Content-Type", "text/plain").entity("400 : pageId and macroId required").build() : Response.ok(new JsonBuilder(entity).toString()).build()
}

private Response okHTML(String html) {
	Response.ok(html).header("Content-Type", "text/html").build()
}

like(httpMethod: "POST", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
	request.parameterMap.each{key, values -> queryParams.add(key, values[0])}
	resp(ike(queryParams){Set likers, liker -> likers+liker})
}

unlike(httpMethod: "POST", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body, HttpServletRequest request  ->
	request.parameterMap.each{key, values -> queryParams.add(key, values[0])}
	resp(ike(queryParams){Set likers,  liker -> likers-liker})
}

like(httpMethod: "GET", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
	UserManager userManager = getComponent(UserManager)
	UserAccessor userAccessor = getComponent(UserAccessor)
	request.getHeader('Accept').toLowerCase().contains('html') ? okHTML('<div id="likes-dialog-body"><ol>' +ike(queryParams, null).collect{ userName ->
		ConfluenceUser user = userManager.getUser(userName) as ConfluenceUser
		if(!user) return "<li><pre>'${userName}'</pre></li>"
		"""<li>
	<div class="avatar-container">
		<a href="/atlassian/display/~${userName}" data-username="${userName}"><img class="like-user-avatar" src="${userAccessor.getUserProfilePicture(user).uriReference}"></a>
	</div>
	<div class="like-user">
		<a class="like-user-link" href="/atlassian/display/~${userName}">${user.fullName}</a>
	</div>
</li>"""
	}.join("")+'</ol></div>') : resp(ike(queryParams, null))
}

Set<String> ike(MultivaluedMap queryParams, Closure<Set> modifyLikers) {
	Long pageId = queryParams.getFirst("pageId") as Long
	String macroId = queryParams.getFirst("macroId")
	if(!pageId || !macroId) return null
	PageManager pageMan = getComponent(PageManager.class)
	ContentPropertyManager propMan = getComponent(ContentPropertyManager.class)
	Page page = pageMan.getPage(pageId)
	String propKey = "com.baloise.likes."+macroId
	String prop = propMan.getTextProperty(page,  propKey) ?: ''
	Set likers = (prop.empty ? [] : prop.split(",") )as HashSet
	if(modifyLikers) likers = modifyLikers(likers,(queryParams.getFirst("userName") ?: AuthenticatedUserThreadLocal.get().name)?.toString()?.toUpperCase())
	propMan.setTextProperty(page, propKey, likers.join(","))
	return likers
}