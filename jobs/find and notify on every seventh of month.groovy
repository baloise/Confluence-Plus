// CRON 0 30 0 7 * ?
import com.atlassian.scheduler.JobRunnerResponse
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method


def httpBuilder = new HTTPBuilder("http://localhost:8090")
def resp = httpBuilder.request(Method.GET, ContentType.JSON) {
	uri.path = "/atlassian/rest/scriptrunner/latest/custom/defaultSpacePermissionStats"
	uri.query = [find: true, notify:true
				]
	response.failure = { resp, reader ->
		return JobRunnerResponse.failed(reader.text.toString())
	}
}

return JobRunnerResponse.success(resp.toString())