
import java.io.File
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.nio.channels.Selector

class ExampleRequestHandler : RequestHandler {
    private fun handleStatic(uriStart: Int, httpRequest: HttpRequest, clientConnection: HttpClientConnection) {

        val sb = StringBuilder(httpRequest.uriEnd - uriStart)
        for (i in uriStart until httpRequest.uriEnd) {
            sb.append(httpRequest.buffer!![i].toChar())
        }
        val path = sb.toString()

        val file = File("src/main/resources", path)
        if (!file.isFile) {
            clientConnection.write("HTTP/1.1 404 Not Found\r\n")
            clientConnection.write("Content-Length: 0\r\n")
            clientConnection.write("\r\n")
        }
        else {
            val randomAccessFile = RandomAccessFile(file, "r")
            val fileChannel = randomAccessFile.channel
            fileChannel.size()

            clientConnection.write("HTTP/1.1 200 OK\r\n")
            clientConnection.write("Content-Length: ${fileChannel.size()}\r\n")
            // TODO: garbage
            if (file.extension == "html") clientConnection.write("Content-Type: text/html\r\n")
            clientConnection.write("\r\n")
            //        clientConnection.write(fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()))

            if (!clientConnection.transferFrom(fileChannel)) {
                // TODO: need some form of continuation
                println("Failed to write file!")
            }
        }
    }

    private fun handleDynamic(httpRequest: HttpRequest, clientConnection: HttpClientConnection) {
        val content = "<html><body>Hello World! <pre>${httpRequest.uri}</pre></body></html>"

        clientConnection.write("HTTP/1.1 200 OK\r\n")
        // TODO: garbage
        clientConnection.write("Content-Length: ${content.length}\r\n")
        clientConnection.write("Content-Type: text/html\r\n")
        clientConnection.write("\r\n")
        clientConnection.write(content)
    }

    override fun handleRequest(httpRequest: HttpRequest, clientConnection: HttpClientConnection) {
        println("*** handling request ***")
        println(httpRequest.uri)

        tailrec fun match(index: Int, matchString: CharArray, matchIndex: Int): Boolean =
            if (matchIndex == matchString.size) true
            else {
                if (httpRequest.buffer!![index].toChar() != matchString[matchIndex]) false
                else match(index + 1, matchString, matchIndex + 1)
            }

        when {
            match(httpRequest.uriStart, "/static/".toCharArray(), 0) -> handleStatic(httpRequest.uriStart + "/static/".length, httpRequest, clientConnection)
            else -> handleDynamic(httpRequest, clientConnection)
        }
    }
}


fun main() {
    val requestHandler = ExampleRequestHandler()

    val server = HttpServer(Selector.open(), InetSocketAddress(8080), requestHandler)
    server.run()
}