import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

import com.atlassian.bandana.BandanaManager
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext
import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.confluence.setup.bandana.KeyedBandanaContext

@BaseScript CustomEndpointDelegate delegate

CONTEXT = new ConfluenceBandanaContext("com.bal.map.k.v")
private ConfluenceBandanaContext CONTEXT(){CONTEXT as ConfluenceBandanaContext}
bandanaMan = getComponent(BandanaManager.class)
private BandanaManager bandanaMan(){bandanaMan as BandanaManager}

private Response ok(entity){
	Response.ok(new JsonBuilder(entity).toString()).header("Content-Type", "application/json").build()
}

private String load(String key) {
	(bandanaMan().getValue(CONTEXT(), key) ?: '') as String
}

private void save(String key, String value) {
	 bandanaMan().setValue(CONTEXT(), key, value)
}
   
private void reset(String key) {
	bandanaMan().removeValue(CONTEXT(), key)
}

map(httpMethod: "GET") { MultivaluedMap<String, String> queryParams, String body ->
	String key = queryParams.getFirst('key')?.trim()
	
	if(!key) return ok(bandanaMan().getKeys(CONTEXT()))
	if(queryParams.containsKey("reset")) {
		reset(key)
	} else {
		String value = queryParams.getFirst('value')?.trim()
		if(value) {
			save(key,value)
		} 
	}
	
	return ok([
        "key" : key,
        "value" : load(key)
    ])
		
}