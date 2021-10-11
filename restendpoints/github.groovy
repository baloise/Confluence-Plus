import groovy.json.JsonSlurper
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.ResponseBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.pages.PageManager
import com.atlassian.user.UserManager
import com.atlassian.user.User
import com.atlassian.confluence.user.ConfluenceUser

@BaseScript CustomEndpointDelegate delegate
GITHUB_PREFIX = "https://github.com/"
PATH_SEPERATOR = '|'

pageMan = getComponent(PageManager.class)
private PageManager pageMan(){pageMan as PageManager}

uman = getComponent(UserManager.class)
private UserManager uman(){uman as UserManager}

lazyConf = null
private Map<String, String> loadConf(){
	if(lazyConf) return lazyConf
    Page page = pageMan().getPage('GITHUB', 'UserMap')
	lazyConf = new XmlSlurper().parseText("<p>${page.bodyAsString}</p>").table.tbody.tr.collectEntries{[(it.th.toString().trim()) : (it.td.toString().trim())]}
}

lazyAdmin = null
User getAdmin(){
    lazyAdmin ?: (lazyAdmin = uman().getUser('admin'))
}

User mapUser(String ghUserName ){
    String mapped = loadConf()[ghUserName]
    mapped ? uman().getUser(mapped) : admin
}

private Page savePage(Page parentPage, String pageTitel, String content, User author = admin){
    Page child = parentPage.children.find{it.title == pageTitel}
    def saveContext = new com.atlassian.confluence.core.DefaultSaveContext.Builder().updateLastModifier(true).build()
	if(!child) {
		child = new Page()
		child.bodyAsString = content
		child.version = 1
		child.title = pageTitel
		child.space = parentPage.space
		child.parentPage = parentPage
		child.creator = author as ConfluenceUser
        child.lastModifier = author as ConfluenceUser
        child.lastModifierName = author.name
		pageMan().saveContentEntity(child, saveContext)
		pageMan().movePageAsChild(child, parentPage)
	} else {
		if(child.bodyAsString == content) return child
		pageMan().<Page>saveNewVersion(child, new com.atlassian.confluence.core.Modification<Page>() {
			public void modify(Page current) {
				current.bodyAsString = content
                current.lastModifier = author
                current.lastModifierName = author.name
			}
	   }, saveContext)
	}
	 return child
}

private Page getFolder(Page parent, List<String> path, List<String> parentPath = []) {
	List<String> myPath = parentPath+path[0]
	Page page = parent.children.find{it.title == path[0]} ?: savePage(parent, myPath.join(PATH_SEPERATOR), "")
	  return path.size() == 1 ? page : getFolder(page, path[1..-1], myPath)
}
private Page getHomePage() {pageMan().getPage("GITHUB", "Home")}


private def writePage(String url, String html, User author) {
	List<String> path = url.split('/')[0..-2]
	Page page = savePage(getFolder(homePage, path),url.replaceAll('/',PATH_SEPERATOR),html, author)
    getComponent(com.atlassian.confluence.core.ContentPropertyManager.class).setStringProperty(page, "com.baloise.classification", "Public")
    return page
}

String scrapePage(String url){
	 def httpBuilder = withProxy(new HTTPBuilder(GITHUB_PREFIX))
	def gitResp =  httpBuilder.request(Method.GET, ContentType.HTML) {
		uri.path = url
	}
	gitResp = gitResp.'**'.find {
		it.@class.toString().contains('markdown-body')
	}
	def writer = new StringWriter()
	groovy.xml.XmlUtil.serialize(gitResp, writer)
	writer.toString().replace('<A href="/', '<A href="https://github.com/')
}

private def deletePage(String url) {
	pageMan().trashPage(pageMan().getPage("GITHUB", url.replaceAll('/',PATH_SEPERATOR)))
}

boolean isDoc(String path) {path.toLowerCase().endsWith(".md") || path.toLowerCase().endsWith(".adoc")}

githubMirror(httpMethod: "POST") { MultivaluedMap queryParams, String body, HttpServletRequest request ->
	def json = new JsonSlurper().parseText(body)
	def resp = body
	//savePage(getFolder(homePage, ["JSON"]), "JSON-"+System.currentTimeMillis(), body)
	if(json.pages) {
		resp = json.pages
		json.pages.each { page ->
			String url = page.html_url - GITHUB_PREFIX - '/blob/master'
			if(["created", "edited"].contains(page.action)) {
				writePage(url, scrapePage(page.html_url - GITHUB_PREFIX), mapUser(json.sender.login))
			} else if("deleted" == page.action) {
				deletePage(url)
			}
		}
    } else if(json.commits) {
        String branch = json.ref - "refs/heads/"
        //TODO should get the default branch via API instead of hard coding
        if(['main', 'master'].contains(branch)) {
            json.commits.each { commit ->
                ["added", "modified"].each { commit[it].each{ path ->
                        if(isDoc(path)){
                            writePage(json.repository.full_name + '/'+path, scrapePage("https://github.com/${json.repository.full_name}/blob/${branch}/${path}"), mapUser(commit.author.name))            
                        }
                }}
                commit.removed.each { path ->
                     if(isDoc(path)){
                         deletePage(json.repository.full_name + '/'+path)
                     }
                }
            }    
        }
    }
    
	Response.status(200).entity(resp).header("Content-Type", "application/json").build()
}

private String proxyAuth() {"Basic " +Base64.getEncoder().encodeToString((System.getProperty("http.proxyUser")+':'+System.getProperty("http.proxyPassword")).getBytes())}

private HTTPBuilder withProxy(HTTPBuilder httpBuilder) {
	httpBuilder.setHeaders(['Proxy-Authorization' :  proxyAuth()])
	httpBuilder.setProxy(System.getProperty("http.proxyHost"), System.getProperty("http.proxyPort") as int, null)
	httpBuilder
}

