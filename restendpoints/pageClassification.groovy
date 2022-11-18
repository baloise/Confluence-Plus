import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.pages.PageManager
import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.confluence.core.ContentPropertyManager
import com.atlassian.user.UserManager
import com.atlassian.user.User
import com.atlassian.confluence.security.PermissionHelper
import com.atlassian.confluence.security.PermissionManager
import com.atlassian.confluence.user.PersonalInformationManager
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal
import com.atlassian.confluence.user.ConfluenceUser
import com.atlassian.confluence.core.ContentPropertyManager




private Response status(int code, String message) {
	Response.status(code).header("Content-Type", "text/plain").entity("$code : $message" as String).build()
}

private Response ok(JsonBuilder jsonb) {
	Response.ok(jsonb.toString()).header("Content-Type", "application/json").build()
}


@BaseScript CustomEndpointDelegate delegate

pageClassification(httpMethod: "GET", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body ->
    long pageId = queryParams.getFirst("pageId") as long
    PageManager pageMan = getComponent(PageManager.class)
    Page page = pageMan.getPage(pageId)
    
	ConfluenceUser currentUser = AuthenticatedUserThreadLocal.get()
    PermissionHelper phelp = new PermissionHelper(
        getComponent(PermissionManager.class),
        getComponent(PersonalInformationManager.class),
        getComponent(PageManager.class)
    )
	
    String classification = queryParams.getFirst("classification")
    ContentPropertyManager propMan = getComponent(ContentPropertyManager.class)
    
    if(classification) {
        if(!phelp.canEdit(currentUser, page)) return status(403, "Forbidden")
        List valid = ["Public","Internal","Confidential","Secret"]
        if(!valid.contains(classification)) return status(400, "Classification must be one of $valid")
        propMan.setStringProperty(page, "com.baloise.classification", classification) 
        return ok(new JsonBuilder([classification: classification]))
    } else {
        if(!phelp.canView(currentUser, page)) return status(403, "Forbidden")
        return ok(new JsonBuilder([classification: propMan.getStringProperty(page, "com.baloise.classification") ?: "Internal"]))
    }
}
