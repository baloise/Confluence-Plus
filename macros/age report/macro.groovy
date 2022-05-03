import com.atlassian.confluence.labels.*
import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.confluence.pages.Page

String label = parameters.label.replaceAll(/\W+/, "").toLowerCase()

import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.sal.api.ApplicationProperties
import static com.atlassian.sal.api.UrlMode.RELATIVE_CANONICAL
def contextPath =  ScriptRunnerImpl.getOsgiService(ApplicationProperties).getBaseUrl(RELATIVE_CANONICAL)

import com.atlassian.confluence.search.v2.*
import com.atlassian.confluence.search.v2.query.*
import com.atlassian.confluence.search.service.ContentTypeEnum
import com.atlassian.confluence.search.v2.sort.*


SearchManager searchManager = getComponent(SearchManager)
	
final ISearch search = new ContentSearch(BooleanQuery.composeAndQuery([new ContentTypeQuery(ContentTypeEnum.PAGE), new LabelQuery(label)] as Set), ModifiedSort.DESCENDING, null, 0, 999)
now = System.currentTimeMillis()
double ageInDays(long time){
    ( now - time)  / 1000 / 60 / 60 / 24
}
int rint(double d){d.round(0)}
try {
    SearchResults searchResults = searchManager.search(search)
    def ages = []
    String LIs = searchResults.getAll()
    .collect{ res ->
        def aid = ageInDays(res.lastModificationDate.time)
        ages << aid
        """<li><a href="${contextPath}${res.urlPath}">${res.displayTitle}</a> - ${rint(aid)}</li>"""
    }.join("\t\t\t\n")
    def average = ages.sum() / ages.size()
    return """Average age is <b>${rint(average)} days</b><ul>${LIs}</ul>"""
} catch (InvalidSearchException e) {
    // We can't recover from this so we wrap the error in a runtime exception
    throw new RuntimeException(e)
}