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


@BaseScript CustomEndpointDelegate delegate

pageMan = getComponent(PageManager.class)
private PageManager pageMan(){pageMan as PageManager}
GITHUB_PREFIX = "https://github.com/"

private Page savePage(Page parentPage, String pageTitel, String content){
	Page child = parentPage.children.find{it.title == pageTitel}
	if(!child) {
		child = new Page()
		child.bodyAsString = content
		child.version = 1
		child.title = pageTitel
		child.space = parentPage.space
		child.parentPage = parentPage
		child.creator = parentPage.creator
		pageMan().saveContentEntity(child, null)
		pageMan().movePageAsChild(child, parentPage)
	} else {
		if(child.bodyAsString == content) return child
		child.version++
		child.bodyAsString = content
		pageMan().saveContentEntity(child, null)
	}
	 return child
}

private Page getFolder(Page parent, List<String> path, List<String> parentPath = []) {
	List<String> myPath = parentPath+path[0]
	Page page = parent.children.find{it.title == path[0]} ?: savePage(parent, myPath.join('/'), "This is a folder")
	  return path.size() == 1 ? page : getFolder(page, path[1..-1], myPath)
}
private Page getHomePage() {pageMan().getPage("GITHUB", "Home")}


private def writePage(String url, String html) {
	List<String> path = url.split('/')[0..-2]
	savePage(getFolder(homePage, path),url,html)
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
	writer.toString()
}

private def deletePage(String url) {
	pageMan().trashPage(pageMan().getPage("GITHUB", url))
}
	
githubMirror(httpMethod: "POST") { MultivaluedMap queryParams, String body, HttpServletRequest request ->
	def json = new JsonSlurper().parseText(body)
	def resp = body
	if(json.pages) {
		resp = json.pages
		json.pages.each { page ->
			String url = page.html_url - GITHUB_PREFIX - '/blob/master'
			if(["created", "updated"].contains(page.action)) {
				writePage(url, scrapePage(page.html_url - GITHUB_PREFIX))
			} else if("deleted" == page.action) {
				deletePage(url)
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

