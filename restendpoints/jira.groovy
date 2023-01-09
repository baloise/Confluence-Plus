import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap


import com.atlassian.applinks.api.ApplicationLinkService
import com.atlassian.applinks.api.application.jira.JiraApplicationType
import com.atlassian.confluence.event.events.space.SpaceCreateEvent
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.sal.api.net.Response
import com.atlassian.sal.api.net.ResponseException
import com.atlassian.sal.api.net.ResponseHandler
import com.atlassian.sal.api.net.ReturningResponseHandler
import static com.atlassian.sal.api.net.Request.MethodType.*
import com.atlassian.sal.api.net.Request.MethodType
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response as JaxRsResponse
import javax.ws.rs.core.Response.ResponseBuilder

@BaseScript CustomEndpointDelegate delegate

jira(httpMethod: "GET", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
	service(GET, request)
}
jira(httpMethod: "POST", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
	service(POST, request, body)
}
jira(httpMethod: "PUT", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
	service(PUT, request, body)
}
jira(httpMethod: "DELETE", groups: ["confluence-users"]) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
	service(DELETE, request)
}

JaxRsResponse service(MethodType httpMethod, HttpServletRequest request, String body = null ) {
	def appLinkService = ComponentLocator.getComponent(ApplicationLinkService)
	def appLink = appLinkService.getPrimaryApplicationLink(JiraApplicationType)
	def applicationLinkRequestFactory = appLink.createAuthenticatedRequestFactory()
	
	// do not copy all headers over. Only JSON request will pass.
    // Otherwise HTTP status code 503 Service Unavailable will be sent
	def jiraRequest = applicationLinkRequestFactory.createRequest(httpMethod, request.queryString).addHeader("Content-Type", "application/json")
	
	if(body) jiraRequest.entity = body
	
	Response jiraResp = jiraRequest.executeAndReturn({it} as ReturningResponseHandler)
	if(!jiraResp.successful) {
		log.error(""+ jiraResp.statusCode)
		log.error(jiraResp.responseBodyAsString)
	}
	JaxRsResponse.status(jiraResp.statusCode)
		.entity({OutputStream out -> out << jiraResp.responseBodyAsStream} as javax.ws.rs.core.StreamingOutput)
		.build()
}
