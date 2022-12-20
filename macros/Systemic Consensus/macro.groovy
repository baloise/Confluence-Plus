String instanceId = "com-baloise-systemic-consensus-${parameters.subject.replaceAll(/\W/,'-')}"
String entityId = context.entity.idAsString
String subject = parameters.subject
"""
<div class="panel" style="border-color: #dfe1e6;border-style: solid;border-width: 1px;" 
    id= "$instanceId" 
    data-entity-id="$entityId" 
    data-options="${body.replaceAll(/\n/, /\\n/)}" 
    data-subject="$subject">
        $subject<br/>
        <aui-spinner size="medium"></aui-spinner>
</div>
<script>AJS.toInit(function(){refresh(`$instanceId`, `$entityId`, `$subject`,`$body`)})</script>
"""