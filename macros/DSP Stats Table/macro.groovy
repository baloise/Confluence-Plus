import com.atlassian.bandana.BandanaManager
import static com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext.GLOBAL_CONTEXT

private List<Map> loadStats() {
    String statskeyProp = "default.permissions.stats.key"
    BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
    (bandanaManager.getValue(GLOBAL_CONTEXT, statskeyProp) ?: []) as List<Map> 
}
String ret = ''' 
<table>
 <thead>
    <tr>
      <th>Date</th>
      <th>Actual</th>
	  <th>Target</th>
      <th>Ignored</th>
    </tr>
  </thead>
<tbody>
'''
loadStats().each{
    ret += "  <tr><td>${(it.date as Date).format('dd-MM-yyyy HH:mm:ss')}</td><td>${it.actual}</td><td>${it.target-it.actual}</td><td>${it.ignored}</td></tr>"
}

ret + '''</tbody>
</table>'''