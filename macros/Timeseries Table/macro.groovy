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

TreeSet<Long> dates = new TreeSet()

Set<String> names = parameters.names.split(",").collect{ String name ->
    name = name.trim()
    name.endsWith(".unique")? loadUnique(name[0..-8]) : name
}.flatten() as Set

TreeMap<String,TreeMap<Long,Integer>> series = names.collectEntries{ name ->
    TreeMap<Long,Integer> data = load(name)
    dates.addAll(data.keySet())
    ["$name" : data]
} as TreeMap


String ret = ''' 
<table>
 <thead>
    <tr>
      <th>Date</th>
'''
series.keySet().each{
    ret += "<th>$it</th>"
}

ret += '''    </tr>
  </thead>
<tbody>'''
dates.each { date ->
    ret += "  <tr><td>${(new Date(date)).format('dd-MM-yyyy HH:mm:ss')}</td>"
    series.values().each{
        ret += "<td>${it.floorEntry(date)?.value ?: 0}</td>"
    }
    ret += "</tr>"
}
ret +'''</tbody>
</table>'''
