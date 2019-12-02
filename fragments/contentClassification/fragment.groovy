import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.pages.PageManager
import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.confluence.core.ContentPropertyManager
import com.atlassian.user.UserManager
import com.atlassian.user.User
import com.atlassian.confluence.security.PermissionHelper
import com.atlassian.confluence.security.PermissionManager
import com.atlassian.confluence.user.PersonalInformationManager
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal
import com.atlassian.confluence.user.ConfluenceUser
import com.atlassian.confluence.core.ContentPropertyManager
import com.atlassian.confluence.core.ContentPermissionManager

ContentPropertyManager propMan = getComponent(ContentPropertyManager.class)

Page page = context.action.page as Page


ConfluenceUser currentUser = AuthenticatedUserThreadLocal.get()
PermissionHelper phelp = new PermissionHelper(
        getComponent(PermissionManager.class),
        getComponent(PersonalInformationManager.class),
        getComponent(PageManager.class)
)

String classification = propMan.getStringProperty(page, "com.baloise.classification") ?: "Internal";

String warn = '';
if('Confidential' == classification && !getComponent(ContentPermissionManager.class).getInheritedContentPermissions(page))
warn =  'warnUnprotected();'
if(page.id == 1836221614L) 
writer.write('''<script>''');
else
writer.write('''<section id="dialog-classify" class="aui-dialog2 aui-dialog2-medium aui-layer" role="dialog" aria-hidden="true">
    <header class="aui-dialog2-header">
        <h2 class="aui-dialog2-header-main">Data confidentiality classification</h2>
        <button id="dialog-classify-close" class="aui-dialog2-header-close" aria-label="close" style="background-color:transparent; border :none;">
            <span class="aui-icon aui-icon-small aui-iconfont-close-dialog"></span>
        </button>
    </header>
    <div class="aui-dialog2-content" style="padding: 0px;">
       
<div class="aui-tabs horizontal-tabs">
    <ul class="tabs-menu" id="tabs-classify-all">
        <li class="menu-item">
            <a href="#tabs-classify-public" id="button-public"><button class="aui-button" style="background-color:#bad80a">Public</button></a>
        </li>
        <li class="menu-item">
            <a href="#tabs-classify-internal" id="button-internal"><button class="aui-button" style="background-color:#ffef00">Internal</button></a>
        </li>
        <li class="menu-item">
            <a href="#tabs-classify-confidential" id="button-confidential"><button class="aui-button" style="background-color:#ff8c00; color:white;">Confidential</button></a>
        </li>
        <li class="menu-item">
            <a href="#tabs-classify-secret" id="button-secret"><button class="aui-button" style="background-color:#a80000; color:white;">Secret</button></a>
        </li>
 </ul>
    <div class="tabs-pane" id="tabs-classify-public">
        <h2>Public information</h2>
        <p> is intended for publication to the general public. <br/>It is not subject to any restrictions.</p>
    </div>
    <div class="tabs-pane" id="tabs-classify-internal">
        <h2>Internal information</h2>
        <p> is available in principle to all Baloise employees. <br/>Release of such information to third parties is only permitted in the course of normal business activities.</p>
    </div>
    <div class="tabs-pane" id="tabs-classify-confidential">
        <h2>Confidential information</h2>
        <p>is information required for a specific business purpose, and is only 
made available to particular employees entrusted with a related work 
assignment in order for them to carry out their function.</p>
    </div>
    <div class="tabs-pane" id="tabs-classify-secret">
        <h2>Secret information</h2>
        <p> is only available to a tightly limited group of employees.</p>
        <div class="aui-message aui-message-error" style="background: #FFEBE6">
            <p class="title">
                <strong>Classification not permitted!</strong>
            </p>
            <p>Confidential data must not be stored in Confluence, as it requires encrypted storage.</p>
        </div>
 </div>
 </div>

  </div>
    <footer class="aui-dialog2-footer">
        <div class="aui-dialog2-footer-actions">
            <button id="dialog-classify-submit-button" class="aui-button aui-button-primary">Apply</button>
        </div>
    </footer>
</section>
<script>
function AJSMeta(prop){
    return AJS.$('meta[name="ajs-'+prop+'"]').attr("content");
}
function initPageClassification(classification, canEdit) {
    var button = AJS.$("#show-dialog-classify");
    if(button.get().length == 0) {
        button = AJS.$('<button id="show-dialog-classify" class="aui-button">...</button>');
        AJS.$("#system-content-items").after(button);
        button.on('click', function(e) {
            e.preventDefault();
            if("true" != $(this).attr("data-canEdit")) return false;
            resetClassifyDialog();
             AJS.dialog2("#dialog-classify").show();
        });
    }
    button.attr("data-classification", classification);
    button.attr("data-canEdit", canEdit);
    button.css("cursor", canEdit ? "pointer" :  "default")
    setPageClassification(classification);
}
function storePageClassification(classification) {
   var currentClassification = AJS.$("#show-dialog-classify").attr("data-classification");
   if(currentClassification == classification) {
           console.log("not storing current configuration");
        return false;
   }
   console.log("storing classification " + classification);
   AJS.$.ajax({
      url: AJSMeta("context-path")+"/rest/scriptrunner/latest/custom/pageClassification",
      type: "GET",
      data: ({pageId : AJSMeta("page-id"), classification :classification}),
      dataType: "json",
      success: function(msg){
        AJS.$("#show-dialog-classify").attr("data-classification", classification);
       AJS.flag({
            type: 'success',
            body: 'Confidentiality classification set to <b>'+classification+'</b>.',
            close: 'auto',
        }); 
    },
      error : function(msg){
        console.error(msg);
        setPageClassification(currentClassification);
        AJS.flag({
            type: 'error',
            body: 'Confidentiality classification <b>not</b> updated.',
            close: 'auto',
        }); 
      }
  });
}

function setPageClassification(classification) {
   if(classification== "Secret") {
           console.log("ignoring Secret");
        return false;
   }
   var colors = {
        Public : ["#bad80a", "black"],
        Internal : ["#ffef00", "black"],
        Confidential: ["#ff8c00", "white"],
        Secret: ["#a80000", "white"]
   }[classification]
   var button = AJS.$("#show-dialog-classify");
   button.css({
      "background-color": colors[0],
      "color": colors[1]
    });
   button.html(classification);
 }

function warnUnprotected(){
    var warning= AJS.$('<div class="aui-message aui-message-warning"><p class="title"><strong>Confidential information needs to be protected</strong></p><p>Please restrict access to this page.</p></div>');
    AJS.$("#main-content").prepend(warning);
}

function resetClassifyDialog() {
    var classification =  AJS.$("#show-dialog-classify").attr("data-classification");
    AJS.tabs.change(AJS.$('a[href="#tabs-classify-'+classification.toLowerCase()+'"]'));
}

AJS.$("#dialog-classify-submit-button").on('click', function (e) {
    e.preventDefault();
    AJS.dialog2("#dialog-classify").hide();
    var classification =  AJS.$("#show-dialog-classify").text();
     storePageClassification(classification);
});

AJS.$("#tabs-classify-all a").on('click', function (e) {
    AJS.$("#dialog-classify-submit-button").attr("disabled", this.id == "button-secret");
    setPageClassification(AJS.$(this).text());
	AJS.tabs.change(AJS.$(this));
	return false;
});

AJS.$("#dialog-classify-close").on('click', function (e) {
    var classification =  AJS.$("#show-dialog-classify").attr("data-classification");
    setPageClassification(classification);
});
''')
writer.write("""
AJS.toInit(function(){ 
	initPageClassification("${classification}", ${phelp.canEdit(currentUser, page)});${warn}
});
</script>
""")
