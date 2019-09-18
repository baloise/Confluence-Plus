import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

import com.atlassian.bandana.BandanaManager
import com.atlassian.confluence.spaces.SpaceManager
import static com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext.GLOBAL_CONTEXT

@BaseScript CustomEndpointDelegate delegate

private Response status(int code, String message) {
	Response.status(code).header("Content-Type", "text/plain").entity("$code : $message" as String).build()
}

private Response ok(String text) {
	Response.ok(text).header("Content-Type", "text/plain").build()
}

defaultPermissionsSpace(httpMethod: "GET", groups: ["confluence-administrators"]) { MultivaluedMap queryParams, String body ->
   String spacekeyProp = "default.permissions.template.space.key"
   BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
   String newKey = queryParams.getFirst("newKey")
   if(newKey){
     SpaceManager spaceMan = com.atlassian.sal.api.component.ComponentLocator.getComponent(SpaceManager.class)
     if(spaceMan.getSpace(newKey)) {
        bandanaManager.setValue(GLOBAL_CONTEXT, spacekeyProp, newKey)
     	return ok(newKey)       
     } else {
        return status(400, "No space found with key $newKey")
     }
   } else {
       return ok(bandanaManager.getValue(GLOBAL_CONTEXT, spacekeyProp))
   }
}
