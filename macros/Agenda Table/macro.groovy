import groovy.time.*
import groovy.xml.*

"${parameters.startTime}<br>${}<br><pre>${body.replace("<","").replace("/>","")}</pre>"

String format(int minutes){String.format("%02d",minutes/60 as int) + ":" + String.format("%02d", minutes%60)} 
def parse(String minutes){minutes.trim().split('\\D+')}

def(hours, minutes) = parse(parameters.startTime)
int time = 60*Integer.valueOf(hours)+Integer.valueOf(minutes)

def xml = new XmlSlurper().parseText("<p>$body</p>") 

def table = xml.'**'.find { node ->
  node.name() == 'table'
}
warnings = []
if(table) {
  int index = table.tbody.tr.th.findIndexOf{it.text().replace('&nbsp;','').trim().equalsIgnoreCase(parameters.durationColumnName) }
  if(index<0) {
    warnings << "Column '" +parameters.durationColumnName +"' not found"
  } else {
    table.tbody.tr.th
    table.tbody.tr.th[index] = "Time"
    log.error ("1")
    table.tbody.tr.list().tail().each {
        log.error ("- "+ it)
        String colTag = it.td.size() ? 'td' : 'th'
        def node = it[colTag][index]
        int duration = Integer.valueOf(parse(node.text())[0]?:0)
        int startTime = time
        int endTime = time + duration    
        node.replaceBody()
        node.appendNode {
        nobr(format(startTime) + " - " + format(endTime) + (parameters.showDuration.toString().toLowerCase() == "true" ? " / "+duration+"'":''))
        }
        time = endTime
    }
  }
} else {
    warnings << "No table found"
}
warnings = warnings ? '<div class="aui-message aui-message-warning"><p><ul><li>'+warnings.join("</li><li>")+'</li></ul></p></div>' : ''
warnings+XmlUtil.serialize( new StreamingMarkupBuilder().bind { mkp.yield xml })

