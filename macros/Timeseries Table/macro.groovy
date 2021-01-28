import com.atlassian.bandana.BandanaManager
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext
import static com.atlassian.sal.api.component.ComponentLocator.getComponent


CONTEXT = new ConfluenceBandanaContext("com.bal.time.series")
private ConfluenceBandanaContext CONTEXT(){CONTEXT as ConfluenceBandanaContext}
CONTEXT_UNIQUE = new ConfluenceBandanaContext("com.bal.time.series.unique")
private ConfluenceBandanaContext CONTEXT_UNIQUE(){CONTEXT_UNIQUE as ConfluenceBandanaContext}

bandanaMan = getComponent(BandanaManager.class)
private BandanaManager bandanaMan(){bandanaMan as BandanaManager}
    
private TreeMap<Long,Integer> load(String name) {
	(bandanaMan().getValue(CONTEXT(), name) ?: [:]) as TreeMap<Long,Integer>   
}

private TreeSet<String> loadUnique(String name) {
    (bandanaMan().getValue(CONTEXT_UNIQUE(), name) ?: []) as TreeSet<String>
}

private String render(String name){
    """<table>
     <thead>
        <tr>
          <th>Date</th>
          <th>${name}</th>
    </tr>
      </thead>
    <tbody>""" +
    load(name).collect{ date, value ->
        " <tr><td>${(new Date(date)).format('dd-MM-yyyy HH:mm:ss.SSS')}</td><td>${value}</td></tr>"
    }.join("") +'</tbody></table>' 
}

parameters.names.split(",").collect{ String name ->
    name = name.trim()
    name.endsWith(".unique")? loadUnique(name[0..-8]) : name
}.flatten().collect{name-> render(name as String)}.join("")
