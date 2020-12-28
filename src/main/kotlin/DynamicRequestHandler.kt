
import HttpUtils.NotFoundHandler
import HttpUtils.response
import collections.PrefixTree
import collections.PrefixTree.Companion.evaluate
import java.net.InetSocketAddress
import java.nio.channels.Selector

class DynamicRequestHandler(routes: Map<String, RequestHandler>, private val defaultHandler: RequestHandler = NotFoundHandler) : RequestHandler {
    private val prefixTree = PrefixTree<RequestHandler>(routes)

    override fun handleRequest(httpRequest: HttpRequest, clientConnection: HttpClientConnection) {
        val handler = prefixTree.evaluate(length = httpRequest.uriEnd - httpRequest.uriStart, index = 0) { index -> httpRequest.buffer!!.get(httpRequest.uriStart + index).toChar() }
        (handler ?: defaultHandler).handleRequest(httpRequest, clientConnection)
    }
}

object MimeType {
    const val TextHtml = "text/html"
    const val TextJson = "text/json"
}

data class HttpStatus(val code: Int, val reason: String) {
    companion object {
        val OK = HttpStatus(200, "OK")
        val NotFound = HttpStatus(404, "Not Found")
        val ImATeapot = HttpStatus(418 , "I'm a teapot")
    }
}

object HttpUtils {
    fun HttpClientConnection.response(content: String? = null, status: HttpStatus = HttpStatus.OK, contentType: String = MimeType.TextHtml) {
        write("HTTP/1.1 ")
        write(status.code)
        write(' ')
        write(status.reason)
        write("\r\n")

        if (content != null) {
            contentLength(content.length)

            write("Content-Type: \r\n")
            write(contentType)
            write("\r\n")

            write("\r\n")
            write(content)
        }
        else {
            contentLength(0)
            write("\r\n")
        }
    }

    private fun HttpClientConnection.contentLength(length: Int) {
        write("Content-Length: ")
        write(length)
        write("\r\n")
    }

    val NotFoundHandler = RequestHandler { _, c -> c.response(status = HttpStatus.NotFound) }
}

fun main() {
    val requestHandler = DynamicRequestHandler(mapOf(
        "/" to RequestHandler { _, conn -> conn.response("Hello World!") },
        "/health" to RequestHandler { _, conn -> conn.response("{ status: 'ok' }", contentType = MimeType.TextJson) }
    ))

    val server = HttpServer(Selector.open(), InetSocketAddress(8080), requestHandler)
    server.run()
}