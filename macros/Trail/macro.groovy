import com.atlassian.confluence.labels.*
import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.confluence.pages.Page


String label = parameters.topic.replaceAll(/\W+/, "").toLowerCase()
int minLength = 5
if(label.length() < minLength) return """<div class="aui-message aui-message-warning">
    <p>The label '$label' must have a minimum length of $minLength.</p>
</div>"""

Page page = context.entity as Page
boolean displayOnly = Boolean.valueOf(parameters.displayOnly)
if(!displayOnly && !page.labels?.collect{l -> l.name}?.contains(label)) {
    LabelManager labelMan = getComponent(LabelManager.class)
    Label pLabel = labelMan.getLabel(label) ?: labelMan.createLabel(new Label(label, Namespace.GLOBAL)) 
    labelMan.addLabel(page,pLabel)
} 
if(displayOnly && page.labels?.collect{l -> l.name}?.contains(label)) {
    LabelManager labelMan = getComponent(LabelManager.class)
    Label pLabel = labelMan.getLabel(label) ?: labelMan.createLabel(new Label(label, Namespace.GLOBAL)) 
    labelMan.removeLabel(page,pLabel)
}


import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.sal.api.ApplicationProperties
import static com.atlassian.sal.api.UrlMode.RELATIVE_CANONICAL
def contextPath =  ScriptRunnerImpl.getOsgiService(ApplicationProperties).getBaseUrl(RELATIVE_CANONICAL)

import com.atlassian.confluence.search.v2.*
import com.atlassian.confluence.search.v2.query.*
import com.atlassian.confluence.search.service.ContentTypeEnum
import com.atlassian.confluence.search.v2.sort.*

    
String panel(String content){
        String compact = Boolean.valueOf(parameters.compact) ? " compact" : ""
        Boolean.valueOf(parameters.panel) ? "<div class='trailPanel$compact'>$content</div>" : content
}    

String macroName = 'com-baloise-trail'

SearchManager searchManager = getComponent(SearchManager)
	
final ISearch search = new ContentSearch(BooleanQuery.composeAndQuery([new ContentTypeQuery(ContentTypeEnum.PAGE), new MacroUsageQuery(macroName), new LabelQuery(label)] as Set), TitleSort.ASCENDING, null, 0, 999)
try {
    SearchResults searchResults = searchManager.search(search)
    String LIs = searchResults.getAll().findAll{!it.spaceKey.endsWith('Archive')}.collect{ res ->
        String liClass = res.urlPath == page.urlPath ? ' class="aui-nav-selected"' :'' 
        """<li${liClass}><a href="${contextPath}${res.urlPath}">${res.displayTitle}</a></li>"""
    }.join("\t\t\t\n")
    return panel("""<nav class="aui-navgroup aui-navgroup-${parameters.orientation}">
   	 <div class="aui-navgroup-inner">
        <div class="aui-navgroup">
			<div class="aui-nav-heading"><strong>${parameters.topic}</strong></div>
            <ul class="aui-nav" id="trail-${label}">
				${LIs}
            </ul>
        </div>
    </div>
</nav>""")
} catch (InvalidSearchException e) {
    // We can't recover from this so we wrap the error in a runtime exception
    throw new RuntimeException(e)
}