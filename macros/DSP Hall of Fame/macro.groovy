import com.atlassian.bandana.BandanaManager
import static com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext.GLOBAL_CONTEXT

private List<Map> loadStats() {
    String statskeyProp = "default.permissions.stats.key"
    BandanaManager bandanaManager = com.atlassian.sal.api.component.ComponentLocator.getComponent(BandanaManager.class)
    (bandanaManager.getValue(GLOBAL_CONTEXT, statskeyProp) ?: []) as List<Map> 
}

List events = []
long actual = -1
loadStats().each{
    if(it.actual > actual) {
        if(it.user && 'true' != it.user.toLowerCase()) events += "<li>${it.date.format('yyyy.MM.dd HH:mm')}: <b>${it.user}</b> reached <b>${it.actual}</b></li>"
    }
    actual = it.actual
}
if(!events) events += '<li>Be the first ;-)</li>'
"<ul>${events.reverse().join('')}</ul>"
