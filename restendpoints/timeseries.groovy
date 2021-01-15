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

CONTEXT = new ConfluenceBandanaContext("com.bal.time.series")
private ConfluenceBandanaContext CONTEXT(){CONTEXT as ConfluenceBandanaContext}
CONTEXT_UNIQUE = new ConfluenceBandanaContext("com.bal.time.series.unique")
private ConfluenceBandanaContext CONTEXT_UNIQUE(){CONTEXT_UNIQUE as ConfluenceBandanaContext}
bandanaMan = getComponent(BandanaManager.class)
private BandanaManager bandanaMan(){bandanaMan as BandanaManager}

private Response ok(entity){
	Response.ok(new JsonBuilder(entity).toString()).header("Content-Type", "application/json").build()
}

private TreeMap<Long,Integer> load(String name) {
	(bandanaMan().getValue(CONTEXT(), name) ?: [:]) as TreeMap<Long,Integer>
}

private void save(String name, TreeMap<Long,Integer> history) {
	 bandanaMan().setValue(CONTEXT(), name, history)
}
   
private void reset(String name) {
	bandanaMan().removeValue(CONTEXT(), name)
	bandanaMan().removeValue(CONTEXT_UNIQUE(), name)
}

private TreeSet<String> loadUnique(String name) {
    (bandanaMan().getValue(CONTEXT_UNIQUE(), name) ?: []) as TreeSet<String>
}

private int unique(String name, String value) {
	TreeSet<String> values =  loadUnique(name) 
	if(values.add(value)) bandanaMan().setValue(CONTEXT_UNIQUE(), name, values)
	return values.size()
}

private int convert(String name, String value, Integer lastValue) {
	if(value == "inc") 
		return lastValue ? ++lastValue : 1
	if(value.integer) 
		return value as int
	return unique(name, value)
}

private TreeMap<Long,Integer> save(String name, Long date, String value) {
	TreeMap<Long,Integer> history = load(name)
    Integer lastValue = history.lastEntry()?.value
	int converted = convert(name, value, lastValue)
	if(lastValue != converted) {
		history.put(date, converted)
		save(name, history)
	}
	return history
}

timeseries(httpMethod: "GET") { MultivaluedMap<String, String> queryParams, String body ->
	List<String> names = queryParams.get('name')
	
	if(!names) return ok(bandanaMan().getKeys(CONTEXT()))
	if(queryParams.containsKey("reset")) {
		names.each{reset(it)}
		return ok(names.collectEntries{["$it" : [:]]})
	} 
	
	String value = queryParams.getFirst('value')?.trim()
	if(value) {
		return ok(names.collectEntries{["$it" : save(it, (queryParams.getFirst('date') ?: System.currentTimeMillis()) as long, value)]})
	} else if(queryParams.containsKey("unique")) {
        return ok(names.collectEntries{["$it" : loadUnique(it)]})
    } else {
		return ok(names.collectEntries{["$it" : load(it)]})
	}	
}
