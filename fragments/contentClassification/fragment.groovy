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
import com.atlassian.confluence.languages.LocaleManager

i18n = [
	de : [
		 title : 'Klassifizierung der Informationen',
		'public' : '�ffentlich',
		 internal : 'Intern',
		 confidential : 'Vertraulich',
		 secret : 'Geheim',
		 public_title : '�ffentliche Informationen',
		 internal_title : 'Interne Informationen',
		 confidential_title : 'Vertrauliche Informationen',
		 secret_title : 'Geheime Informationen',
		 public_text : ' unterliegen keinen Einschr�nkungen in der Weitergabe und keinen besonderen Schutzvorkehrungen.',
		 internal_text : ' stehen grunds�tzlich allen Mitarbeitenden der Baloise zur Verf�gung.',
		 confidential_text : ' sind alle f�r eine spezifische Gesch�ftst�tigkeit erforderlichen Informationen, die nur bestimmten Mitarbeitenden zur Aus�bung ihrer Funktion zur Verf�gung stehen.',
		 secret_text : ' stehen nur einem eng begrenzten Kreis von Mitarbeitern zur Verf�gung. Ihre Offenlegung h�tte gravierende Konsequenzen f�r die Baloise zur Folge.',
		 nopermit_title : 'Klassifizierung nicht m�glich!',
		 nopermit_text : 'Geheime Informationen d�rfen nicht in Confluence gespeichert werden, da sie Datentr�gerverschl�sselung voraussetzen.',
		 warn_title : 'Vertrauliche Informationen m�ssen gesch�tzt werden.',
		 warn_text : 'Bitte schr�nken Sie den Zugriff auf diese Seite ein.',
		 update_OK : 'Klassifizierung wurde ge�ndert in',
		 update_KO : 'Klassifizierung konnte <b>nicht</b> ge�ndert werden.',
		 apply : 'Speichern',
		 recursive : 'Auf alle Unterseiten anwenden',
	],
	en : [
		title : 'Information classification',
		'public' : 'Public',
		internal : 'Internal',
		confidential : 'Confidential',
		secret : 'Secret',
		public_title : 'Public information',
		internal_title : 'Internal information',
		confidential_title : 'Confidential information',
		secret_title : 'Secret information',
		public_text : ' is intended for publication to the general public. <br/>It is not subject to any restrictions.',
		internal_text : ' is available in principle to all Baloise employees. <br/>Release of such information to third parties is only permitted in the course of normal business activities.',
		confidential_text : 'is information required for a specific business purpose, and is only made available to particular employees entrusted with a related work assignment in order for them to carry out their function.',
		secret_text : ' is only available to a tightly limited group of employees.',
		nopermit_title : 'Classification not permitted!',
		nopermit_text : 'Secret data must not be stored in Confluence, as it requires encrypted storage.',
		warn_title : 'Confidential information needs to be protected',
		warn_text : 'Please restrict access to this page.',
		update_OK : 'Confidentiality classification set to',
		update_KO : 'Confidentiality classification <b>not</b> updated.',
		apply : 'Apply',
		recursive : 'Apply to all sub pages',
	],
]
LocaleManager lman = getComponent(LocaleManager.class)
ConfluenceUser currentUser = AuthenticatedUserThreadLocal.get()
lang = lman.getLocale(currentUser).toString()[0..1]
lang = i18n[lang] ? lang : 'en'
String local(String key) {i18n[lang][key]}

ContentPropertyManager propMan = getComponent(ContentPropertyManager.class)

Page page = context.action.page as Page






PermissionHelper phelp = new PermissionHelper(
		getComponent(PermissionManager.class),
		getComponent(PersonalInformationManager.class),
		getComponent(PageManager.class)
)

String classification = propMan.getStringProperty(page, "com.baloise.classification") ?: "Internal";

String warn = '';
if('Confidential' == classification && !(page.hasContentPermissions() || getComponent(ContentPermissionManager.class).getInheritedContentPermissionSets(page,true)))
warn =  'warnUnprotected();'
writer.write('''<section id="dialog-classify" class="aui-dialog2 aui-dialog2-medium aui-layer" role="dialog" aria-hidden="true">
	<header class="aui-dialog2-header">
		<h2 class="aui-dialog2-header-main">'''+local('title')+'''</h2>
        <button id="dialog-classify-close" class="aui-dialog2-header-close" aria-label="close" style="background-color:transparent; border :none;">
            <span class="aui-icon aui-icon-small aui-iconfont-close-dialog"></span>
        </button>
    </header>
    <div class="aui-dialog2-content" style="padding: 0px;">
       
<div class="aui-tabs horizontal-tabs">
    <ul class="tabs-menu" id="tabs-classify-all">
        <li class="menu-item">
            <a href="#tabs-classify-public" id="button-public" data-classification="Public"><button class="aui-button" style="background-color:#bad80a">'''+local('public')+'''</button></a>
        </li>
        <li class="menu-item">
            <a href="#tabs-classify-internal" id="button-internal" data-classification="Internal"><button class="aui-button" style="background-color:#ffef00">'''+local('internal')+'''</button></a>
        </li>
        <li class="menu-item">
            <a href="#tabs-classify-confidential" id="button-confidential" data-classification="Confidential"><button class="aui-button" style="background-color:#ff8c00; color:white;">'''+local('confidential')+'''</button></a>
        </li>
        <li class="menu-item">
            <a href="#tabs-classify-secret" id="button-secret" data-classification="Secret"><button class="aui-button" style="background-color:#a80000; color:white;">'''+local('secret')+'''</button></a>
        </li>
 </ul>
    <div class="tabs-pane" id="tabs-classify-public">
        <h2>'''+local('public_title')+'''</h2>
        <p>'''+local('public_text')+'''</p>
    </div>
    <div class="tabs-pane" id="tabs-classify-internal">
        <h2>'''+local('internal_title')+'''</h2>
        <p>'''+local('internal_text')+'''</p>
    </div>
    <div class="tabs-pane" id="tabs-classify-confidential">
        <h2>'''+local('confidential_title')+'''</h2>
        <p>'''+local('confidential_text')+'''</p>
    </div>
    <div class="tabs-pane" id="tabs-classify-secret">
        <h2>'''+local('secret_title')+'''</h2>
        <p>'''+local('secret_text')+'''</p>
        <div class="aui-message aui-message-error" style="background: #FFEBE6">
            <p class="title">
                <strong>'''+local('nopermit_title')+'''</strong>
            </p>
            <p>'''+local('nopermit_text')+'''</p>
        </div>
 </div>
 </div>

  </div>
    <footer class="aui-dialog2-footer">
        <div class="aui-dialog2-footer-actions">
            <button id="dialog-classify-submit-button" class="aui-button aui-button-primary">'''+local('apply')+'''</button>
        </div>
		<div class="aui-dialog2-footer-hint">
			<div class="checkbox">
		    	<input class="checkbox" type="checkbox" name="recursiveClassification" id="recursiveClassification">
		        <label for="recursiveClassification">'''+local('recursive')+'''</label>
		    </div>
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
   var recursive = AJS.$("#recursiveClassification").prop('checked');
   console.log("storing classification " + classification, "recursive "+recursive);
   AJS.$.ajax({
      url: AJSMeta("context-path")+"/rest/scriptrunner/latest/custom/pageClassification",
      type: "GET",
      data: ({pageId : AJSMeta("page-id"), classification : classification, recursive : recursive}),
      dataType: "json",
      success: function(msg){
        AJS.$("#show-dialog-classify").attr("data-classification", classification);
       AJS.flag({
            type: 'success',
            body: "'''+local('update_OK')+''' <b>"+clabels.get(classification)+"</b>.",
            close: 'auto',
        }); 
    },
      error : function(msg){
        console.error(msg);
        setPageClassification(currentClassification);
        AJS.flag({
            type: 'error',
            body: "'''+local('update_KO')+'''",
            close: 'auto',
        }); 
      }
  });
}

var clabels = new Map();
clabels.set("Public", "'''+local('public')+'''");
clabels.set("Internal", "'''+local('internal')+'''");
clabels.set("Confidential", "'''+local('confidential')+'''");
clabels.set("Secret", "'''+local('secret')+'''");

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
   button.attr("data-classification-page",classification);
   button.html(clabels.get(classification));
 }

function warnUnprotected(){
    if($("#warnUnprotected").length) return;
    var warning= AJS.$('<div class="aui-message aui-message-warning" id="warnUnprotected"><p class="title"><strong>'''+local('warn_title')+'''</strong></p><p>'''+local('warn_text')+'''</p></div>');
    AJS.$("#main-content").prepend(warning);
}

function resetClassifyDialog() {
    var classification =  AJS.$("#show-dialog-classify").attr("data-classification");
    AJS.tabs.change(AJS.$('a[href="#tabs-classify-'+classification.toLowerCase()+'"]'));
}

AJS.$("#dialog-classify-submit-button").on('click', function (e) {
    e.preventDefault();
    AJS.dialog2("#dialog-classify").hide();
    var classification =  AJS.$("#show-dialog-classify").attr("data-classification-page");
    storePageClassification(classification);
});

AJS.$("#tabs-classify-all a").on('click', function (e) {
    AJS.$("#dialog-classify-submit-button").attr("disabled", this.id == "button-secret");
    setPageClassification(AJS.$(this).attr("data-classification"));
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
