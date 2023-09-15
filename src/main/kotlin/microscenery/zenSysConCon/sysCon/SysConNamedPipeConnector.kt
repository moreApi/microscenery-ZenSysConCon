package microscenery.zenSysConCon.sysCon

import fromScenery.lazyLogger
import microscenery.lightSleepOnCondition
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class SysConNamedPipeConnector {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))
    private val pipe = RandomAccessFile("""\\.\pipe\SysCon2ControlInterface""", "rw")


    fun sendRequest(command: String, params: List<Any> = emptyList()): List<String> {
        logger.info("Sending command: $command Params: ${params.joinToString()}")
        var request = command.toCommand()
        params.forEach {
            request += when (it){
                is String -> it.toParam()
                is Int -> it.toParam()
                is Float -> it.toParam()
                is Boolean -> it.toParam()
                else -> {
                    throw IllegalArgumentException("Unknown type for byte conversion ${it.javaClass.name}")
                }
            }
        }
        pipe.write(request)

        lightSleepOnCondition(5000) { pipe.length() != 0L }

        val resp = mutableListOf<String>()
        while (pipe.length() != 0L) {
            //todo seperate header
            resp.add(pipe.readLine())
        }
        return resp
    }

    fun close(){
        pipe.close()
    }

    companion object{

        // Transforms a native string to an ASCI-string
        private fun String.toCommand(): ByteArray {
            val a = this.map { it.code.toByte()}
                .plus(13).plus(10)// end line chars
            return a.toByteArray()
        }

        private fun Int.toParam(): ByteArray{
            val buffer = ByteArray(4)
            buffer[0] = (this shr 0).toByte()
            buffer[1] = (this shr 8).toByte()
            buffer[2] = (this shr 16).toByte()
            buffer[3] = (this shr 24).toByte()
            return buffer
        }

        private fun Float.toParam(): ByteArray{
            return ByteBuffer.allocate(4).putFloat(this).array()
        }

        private fun String.toParam(): ByteArray{
            return length.toParam().plus(this.map { it.code.toByte() })
        }

        private fun Boolean.toParam(): ByteArray{
            return ByteArray(1){if (this) 1.toByte() else 0.toByte()}
        }

    }
}