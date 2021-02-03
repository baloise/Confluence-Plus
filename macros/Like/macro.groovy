import com.atlassian.confluence.xhtml.api.*
import com.atlassian.confluence.content.render.xhtml.storage.macro.*   
import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.confluence.core.ContentPropertyManager
import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal
import com.atlassian.confluence.user.ConfluenceUser

// next confluence version
//String macroId = (context.getProperty("macroDefinition") as MacroDefinition).macroIdentifier.orElse(null)?.id
String macroId = (context.getProperty("macroDefinition") as MacroDefinition).macroId.getOrNull()?.id

if(!macroId) return "no id"
ContentPropertyManager propMan = getComponent(ContentPropertyManager.class)
Page page = context.entity as Page
String propKey = "com.baloise.likes."+macroId
String prop = propMan.getTextProperty(page,  propKey) ?: ''
Set likers = (prop.empty ? [] : prop.split(",") )as HashSet
ConfluenceUser currentUser = AuthenticatedUserThreadLocal.get()
boolean liking = likers.contains(currentUser.name.toUpperCase())
String ike =  liking ? " Unlike" : " Like"
String html = """<button class="aui-button aui-button-subtle" id="${macroId}b"><span class="aui-icon aui-icon-small aui-iconfont-like">${ike}</span><span>${ike}</span></button>"""
return "${html}<aui-badge id=\"${macroId}c\" style=\"cursor: ${likers.size()? "pointer" : "auto"};\">${likers.size()}</aui-badge><script>AJS.toInit(function() {initLikeMacro('${macroId}', ${liking});});</script>"    
