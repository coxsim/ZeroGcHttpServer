class Logger {
    fun info(arg1: Any?) = log("INFO", arg1)
    fun info(arg1: Any?, arg2: Any?) = log("INFO", arg1, arg2)
    fun info(arg1: Any?, arg2: Any?, arg3: Any?) = log("INFO", arg1, arg2, arg3)
    fun info(arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?) = log("INFO", arg1, arg2, arg3, arg4)
    fun debug(arg1: Any?) = log("DEBUG", arg1)
    fun debug(arg1: Any?, arg2: Any?) = log("DEBUG", arg1, arg2)
    fun debug(arg1: Any?, arg2: Any?, arg3: Any?) = log("DEBUG", arg1, arg2, arg3)
    fun debug(arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?) = log("DEBUG", arg1, arg2, arg3, arg4)
    fun trace(arg1: Any?) = log("TRACE", arg1)
    fun trace(arg1: Any?, arg2: Any?) = log("TRACE", arg1, arg2)
    fun trace(arg1: Any?, arg2: Any?, arg3: Any?) = log("TRACE", arg1, arg2, arg3)
    fun trace(arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?) = log("TRACE", arg1, arg2, arg3, arg4)
    fun trace(arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?) = log("TRACE", arg1, arg2, arg3, arg4, arg5)
    fun trace(arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?, arg6: Any?) = log("TRACE", arg1, arg2, arg3, arg4, arg5, arg6)
    fun trace(arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?, arg6: Any?, arg7: Any?) = log("TRACE", arg1, arg2, arg3, arg4, arg5, arg6, arg7)
    fun trace(arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?, arg6: Any?, arg7: Any?, arg8: Any?) = log("TRACE", arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8)

    private fun printLevel(level: String) { print('['); print(level); print("] "); }
    private fun log(level: String, message: Any?) { printLevel(level); println(message) }
    private fun log(level: String, arg1: Any?, arg2: Any?) { printLevel(level); print(arg1); println(arg2) }
    private fun log(level: String, arg1: Any?, arg2: Any?, arg3: Any?) { printLevel(level); print(arg1); print(arg2); println(arg3) }
    private fun log(level: String, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?) { printLevel(level); print(arg1); print(arg2); print(arg3); println(arg4) }
    private fun log(level: String, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?) { printLevel(level); print(arg1); print(arg2); print(arg3); print(arg4); println(arg5)  }
    private fun log(level: String, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?, arg6: Any?) { printLevel(level); print(arg1); print(arg2); print(arg3); print(arg4); print(arg5); println(arg6)  }
    private fun log(level: String, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?, arg6: Any?, arg7: Any?) { printLevel(level); print(arg1); print(arg2); print(arg3); print(arg4); print(arg5); print(arg6); println(arg7)  }
    private fun log(level: String, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?, arg6: Any?, arg7: Any?, arg8: Any?) { printLevel(level); print(arg1); print(arg2); print(arg3); print(arg4); print(arg5); print(arg6); print(arg7); println(arg8)  }



    fun error(message: String, e: Exception) {
        print("[ERROR] ")
        println(message)
        e.printStackTrace()
    }
}