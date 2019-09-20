import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

import com.atlassian.bandana.BandanaManager
import static com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext.GLOBAL_CONTEXT

@BaseScript CustomEndpointDelegate delegate


private Response ok(String text) {
	Response.ok(text).header("Content-Type", "text/plain").build()
}

defaultPermissionsPage(httpMethod: "GET", groups: ["confluence-administrators"]) { MultivaluedMap queryParams, String body ->
   String pageKeyProp = "default.permissions.template.page.key"
   BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
   String newKey = queryParams.getFirst("newKey")
   if(newKey){
      bandanaManager.setValue(GLOBAL_CONTEXT, pageKeyProp, newKey)
     return ok(newKey)       
   } else {
       return ok(bandanaManager.getValue(GLOBAL_CONTEXT, pageKeyProp)?: "Confluence+Default+Space+Permissions") 
   }
}
