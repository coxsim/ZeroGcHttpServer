import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*

interface Selectable {
    fun handleSelect(key: SelectionKey, selector: Selector)
}

data class HttpClientConnection(
    private var _selector: Selector? = null,
    private var _socketChannel: SocketChannel? = null,
    private var _bufferPool: ObjectPool<ByteBuffer>? = null,
    private var _requestHandler: RequestHandler? = null
) : Selectable {
    private val selector: Selector
        get() = _selector!!
    private val socketChannel: SocketChannel
        get() = _socketChannel!!
    private val bufferPool: ObjectPool<ByteBuffer>
        get() = _bufferPool!!
    private val requestHandler: RequestHandler
        get() = _requestHandler!!

    companion object {
        private val consoleLogger = Logger()

        const val CarriageReturn = '\r'.toByte()
        const val LineFeed = '\n'.toByte()
    }

    inner class ConnectionLogger {
        fun trace(message: Any?) = consoleLogger.trace(socketChannel.remoteAddress, ": ", message)
        fun trace(message: Any?, arg2: Any?) = consoleLogger.trace(socketChannel.remoteAddress, ": ", message, arg2)
        fun trace(message: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?) = consoleLogger.trace(socketChannel.remoteAddress, ": ", message, arg2, arg3, arg4, arg5)
        fun debug(message: Any?) = consoleLogger.debug(socketChannel.remoteAddress, ": ", message)
        fun info(message: Any?) = consoleLogger.info(socketChannel.remoteAddress, ": ", message)
    }

    private val logger = ConnectionLogger()

    private var _requestBuffer: ObjectPool<ByteBuffer>.PooledObject? = null
    private val requestBuffer: ByteBuffer
        get() = _requestBuffer!!.obj

    fun init(selector: Selector, socketChannel: SocketChannel, bufferPool: ObjectPool<ByteBuffer>, requestHandler: RequestHandler) {
        _selector = selector
        _socketChannel = socketChannel
        _bufferPool = bufferPool
        _requestHandler = requestHandler

        _requestBuffer = bufferPool.acquire()

        socketChannel.configureBlocking(false)
        socketChannel.register(selector, SelectionKey.OP_READ, this)
    }


    private fun handleConnectionClosed(key: SelectionKey) {
        key.cancel()
        socketChannel.close()
    }

    interface ReadingState {
        fun handleBytesRead(startPos: Int, endPos: Int): ReadingState
    }

    interface LineReader {
        fun handleLine(startPos: Int, endPos: Int): LineReader
    }

    private val awaitingCr = AwaitingCr()
    private val awaitingLf = AwaitingLf()

    private val requestLineReader = RequestLineReader()
    private val headerLineReader = HeaderLineReader()

    private var state: ReadingState = awaitingCr
    private var lineReader: LineReader = requestLineReader

    private val request = HttpRequest()

    override fun handleSelect(key: SelectionKey, selector: Selector) {
        val startPos = requestBuffer.position()

//        requestBuffer.mark()


        logger.trace(requestBuffer)
        val bytesRead = socketChannel.read(requestBuffer)
        logger.trace("handleSelect bytesRead = ", bytesRead)


        if (bytesRead < 0) {
            handleConnectionClosed(key)
        } else if (bytesRead > 0) {
            requestBuffer.flip()
            state = state.handleBytesRead(startPos = startPos, endPos = startPos + bytesRead)
            // TODO: check if state is 'new' and reset buffer if so
//      requestBuffer.flip()

            requestBuffer.clear()
        }
    }

    inner class AwaitingCr : ReadingState {
        private var lineStartPos = 0
        fun init(lineStartPos: Int) {
            this.lineStartPos = lineStartPos
        }

        override fun handleBytesRead(startPos: Int, endPos: Int): ReadingState {
            logger.trace("AwaitingCr.handleBytesRead(startPos = ", startPos, ", endPos = ", endPos, ")")

            tailrec fun loop(index: Int): ReadingState {
                return if (index >= endPos) this
                else {
                    val ch = requestBuffer.get(index)
                    print(ch.toChar())

                    if (requestBuffer.get(index) == CarriageReturn) {
                        logger.debug("CarriageReturn")

                        awaitingLf.init(lineStartPos = lineStartPos)
                        awaitingLf.handleBytesRead(startPos = index + 1, endPos = endPos)
                    } else loop(index + 1)
                }
            }
            return loop(startPos)
        }
    }

    inner class AwaitingLf : ReadingState {
        private var lineStartPos = 0
        fun init(lineStartPos: Int) {
            this.lineStartPos = lineStartPos
        }

        override fun handleBytesRead(startPos: Int, endPos: Int): ReadingState {
            logger.trace("AwaitingLf.handleBytesRead(startPos = ", startPos, ", endPos = ", endPos, ")")

            tailrec fun loop(index: Int): ReadingState {
                return if (index >= endPos) this
                else {
                    val ch = requestBuffer.get(index)
                    print(ch.toChar())

                    if (requestBuffer.get(index) == LineFeed) {
                        logger.debug("LineFeed")

                        lineReader = lineReader.handleLine(startPos = lineStartPos, endPos = index - 1)

                        awaitingCr.init(if (index+1 == requestBuffer.limit()) 0 else index+1)
                        awaitingCr.handleBytesRead(index+1, endPos)
                    } else loop(index + 1)
                }
            }
            return loop(startPos)
        }
    }

    inner class RequestLineReader : LineReader {
        override fun handleLine(startPos: Int, endPos: Int): LineReader {
            request.buffer = requestBuffer
            when (requestBuffer[startPos].toChar()) {
                'G' -> {
                    assert(requestBuffer[startPos + 1] == 'E'.toByte())
                    assert(requestBuffer[startPos + 2] == 'T'.toByte())
                    assert(requestBuffer[startPos + 3] == ' '.toByte())
                    request.method = HttpMethod.GET
                    request.uriStart = startPos + 4
                    expectRequestUri(endPos)
                }
                'P' -> {
                    assert(requestBuffer[startPos + 1] == 'O'.toByte())
                    assert(requestBuffer[startPos + 2] == 'S'.toByte())
                    assert(requestBuffer[startPos + 3] == 'T'.toByte())
                    assert(requestBuffer[startPos + 4] == ' '.toByte())
                    request.method = HttpMethod.POST
                    request.uriStart = startPos + 5
                    expectRequestUri(endPos)
                }
            }

            return headerLineReader
        }

        private fun expectRequestUri(lineEndPos: Int) {
            tailrec fun loop(index: Int) {
                return if (index < lineEndPos) {
                    if (requestBuffer.get(index) == ' '.toByte()) {
                        request.uriEnd = index
                        expectVersion(lineEndPos)
                    } else loop(index + 1)
                } else error("Invalid request") // TODO: send error code
            }
            loop(request.uriStart)
        }

        private fun expectVersion(lineEndPos: Int) {
            assert(requestBuffer.get(request.uriEnd + 1) == 'H'.toByte())
            assert(requestBuffer.get(request.uriEnd + 2) == 'T'.toByte())
            assert(requestBuffer.get(request.uriEnd + 3) == 'T'.toByte())
            assert(requestBuffer.get(request.uriEnd + 4) == 'P'.toByte())
            assert(requestBuffer.get(request.uriEnd + 5) == '/'.toByte())
            request.httpVersionMajor = requestBuffer.get(request.uriEnd + 5).toChar() - '0'
            assert(requestBuffer.get(request.uriEnd + 7) == '.'.toByte())
            request.httpVersionMinor = requestBuffer.get(request.uriEnd + 8).toChar() - '0'

//      logger.debug(request.uri)
//
//      logger.debug("After HTTP version:")
//      for (i <- request.uriEnd+9 until lineEndPos) {
//        print(requestBuffer.get(i).toChar)
//      }
//      println()
//      logger.debug("^")

            assert(lineEndPos == request.uriEnd + 9) { "$lineEndPos != ${request.uriEnd + 9}" }
        }
    }

    inner class HeaderLineReader : LineReader {
        override fun handleLine(startPos: Int, endPos: Int) = if (endPos == startPos) {
            // end of header
            //

            // assume there's no body
            requestHandler.handleRequest(request, this@HttpClientConnection)
            requestLineReader
        } else {
            // TODO: pool
            val header = Header().apply {
                buffer = requestBuffer
                start = startPos
                end = endPos

                prev = request.lastHeader
            }

            request.lastHeader?.let { it.next = header }
            if (request.firstHeader == null) request.firstHeader = header
            request.lastHeader = header

            headerLineReader
        }
    }



    // ********************
    // writing logic
    // ********************


    // TODO: accept pooled buffer
    fun write(outputBuffer: ByteBuffer): Boolean {
        socketChannel.write(outputBuffer)
        return !outputBuffer.hasRemaining()
    }

    fun transferFrom(fromChannel: FileChannel): Boolean {
        val fileSize = fromChannel.size()
        val bytesTransferred = fromChannel.transferTo(0, fileSize, socketChannel)
        return bytesTransferred < fileSize
    }

    fun write(value: String): Boolean = write(ByteBuffer.wrap(value.encodeToByteArray()))


}

interface RequestHandler {
    fun handleRequest(httpRequest: HttpRequest, clientConnection: HttpClientConnection)
}

class HttpServer(private val selector: Selector,
                 private val listenSocketAddress: SocketAddress,
                 private val requestHandler: RequestHandler) : AutoCloseable {
    private val logger = Logger()

    private val bufferPool = ObjectPool<ByteBuffer>(10) { ByteBuffer.allocateDirect(1024) }
    private val connectionPool = ObjectPool(10) { HttpClientConnection() }

    private val serverSocketChannel = ServerSocketChannel.open()

    @Volatile
    private var running: Boolean = true

    fun run() {
        serverSocketChannel.bind(listenSocketAddress)
        serverSocketChannel.configureBlocking(false)
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)

        while (running) {
            if (selector.select() <= 0) continue
            val keys = selector.selectedKeys()

            try {
                val it = keys.iterator()
                while (it.hasNext()) {
                    val key = it.next()
                    if (key.isAcceptable) {
                        val socketChannel = serverSocketChannel.accept()
                        logger.debug("Accepting connection from ${socketChannel.remoteAddress}")
                        // TODO client connection needs releasing back to pool
                        connectionPool.acquire().apply { obj.init(selector, socketChannel, bufferPool, requestHandler) }
                    }
                    else {
                        try {
                            (key.attachment() as Selectable).handleSelect(key, selector)
                        }
                        catch (e: Exception) {
                            logger.error("Failed to handle select for channel ${key.channel()}", e)
                        }
                    }
                }
            }
            finally {
                keys.clear()
            }
        }
    }

    override fun close() {
        running = false
    }
}



enum class HttpMethod { GET, POST }

data class HttpRequest(
    var buffer: ByteBuffer? = null,
    var method: HttpMethod? = null,
    var uriStart: Int = 0,
    var uriEnd: Int = 0,
    var httpVersionMajor: Int = 0,
    var httpVersionMinor: Int = 0,
    var firstHeader: Header? = null,
    var lastHeader: Header? = null
) {
    val uri: String
        get() = StringBuilder(uriEnd - uriStart).apply {
            for (i in uriStart until uriEnd) append(buffer!![i].toChar())
        }.toString()
}

data class Header(
    var buffer: ByteBuffer? = null,
    var start: Int = 0,
    var end: Int = 0,

    var next: Header? = null,
    var prev: Header? = null
) {
    override fun toString() = StringBuilder(end - start).apply {
        for (i in start until end) append(buffer!![i].toChar())
    }.toString()
}


fun main() {
    val requestHandler = ComplexRequestHandler()

    val server = HttpServer(Selector.open(), InetSocketAddress(8080), requestHandler)
    server.run()
}
