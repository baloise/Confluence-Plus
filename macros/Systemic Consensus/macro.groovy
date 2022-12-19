import java.util.SortedSet
import java.util.TreeSet
import com.atlassian.confluence.core.ContentPropertyManager
import static com.atlassian.sal.api.component.ComponentLocator.getComponent
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal
import com.atlassian.user.UserManager

ContentPropertyManager propMan = getComponent(ContentPropertyManager.class)
String instanceId = "com-baloise-systemic-consensus-${parameters.subject.replaceAll(/\W/,'-')}"
String propKey = "$instanceId-userNames"
String tmp = propMan.getTextProperty(context.entity, propKey)
SortedSet<String> userNames = tmp ? tmp.split(',') as TreeSet : new TreeSet()
SortedSet<String> options = body.split("\r?\n") as TreeSet
options.add("Status Quo")

String createUserLink(String userName) {
    String currentUserName =  AuthenticatedUserThreadLocal.get().name
    UserManager uMan = getComponent(UserManager)
    """<a class="confluence-userlink user-mention ${currentUserName == userName ? 'current-user-mention' : '' }" data-username="$userName" href="/atlassian/display/~$userName" data-linked-resource-type="userinfo">${uMan.getUser(userName).fullName}</a>"""
}

String createVoteField(String userName, String option) {
    String currentUserName =  AuthenticatedUserThreadLocal.get().name
    Integer vote = getVote(userName, option)
    currentUserName == userName ? 
    """<aui-select
            id="vote-$option"
            placeholder="How high is your resistance?"
        >""" + (0..10).collect{"<aui-option ${vote == it ? 'selected' : '' }>$it</aui-option>"}.join('')+"</aui-select>"  :  vote
}

Integer getVote(userName,option) {
    return 2
}

String createTableBody(Set<String> userNames, Set<String> options){
     Map<String,List<String>> voteFields = [:]
    userNames.each { userName -> 
        voteFields.put(  userName
    , options.collect { createVoteField(userName
    ,it) })
    }
    voteFields ? 
    voteFields.entrySet().collect{vote -> 
            "<td>${createUserLink(vote.key)}</td><td>"+vote.value.join("</td><td>")+"</td>"
    }.join('</tr><tr>') : "<td colspan=\"${options.size()+1}\"><i>no votes yet</i></td>"
}


String currentuserName = AuthenticatedUserThreadLocal.get().name as String
String currentToggleIcon = userNames.contains(currentuserName) ? 'aui-iconfont-remove' : 'aui-iconfont-add'


"""
<div class="panel" style="border-color: #dfe1e6;border-style: solid;border-width: 1px;" id="$instanceId" data-entity-id="${context.entity.idAsString}" data-subject="${parameters.subject}"><div class="panelContent">
 <div  style="position: relative;">
 <div class="buttons-container" style="position: absolute; right: 8px; top: 8px; z-index: 100;">
        <div class="buttons">
        <button class="aui-button" data-instance-id="$instanceId" name="addRemoveParticipantToVote"><span class="aui-icon aui-icon-small $currentToggleIcon"></span></button>
        <button class="aui-button" data-instance-id="$instanceId"  name="refreshVote"><span class="aui-icon aui-icon-small aui-iconfont-refresh">Refresh</span></button>
        </div>
  </div>
  </div>
<form class="aui">
<table class="aui">
  <thead>
        <tr>
        <th>${parameters.subject}</th>
        <th>${options.join("</td><td>")}</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            ${createTableBody(userNames, options)}
        </tr>
    </tbody>
</table>
</form>
</div></div>
"""