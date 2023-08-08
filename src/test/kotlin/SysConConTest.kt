package microscenery.example

import org.slf4j.LoggerFactory
import java.io.RandomAccessFile
import java.nio.ByteBuffer


class SysConConTest {

    companion object{

        val logger = LoggerFactory.getLogger("PipeTest")

        // Transforms a native string to an SCI-string
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

        private fun RandomAccessFile.sendRequest(command: String, params: List<Any> = emptyList()): List<String> {
            var request = command.toCommand()
            params.forEach {
                request += when (it){
                    is String -> it.toParam()
                    is Int -> it.toParam()
                    is Float -> it.toParam()
                    else -> {
                        throw IllegalArgumentException("Unknown type for byte conversion ${it.javaClass.name}")
                    }
                }


            }
            this.write(request)
            logger.info("send $command with ${params.size} params")
            Thread.sleep(500)

            val resp = mutableListOf<String>()
            while (this.length() != 0L) {
                //todo seperate header
                resp.add(this.readLine())
            }
            return resp
        }

        private fun List<String>.printResponse() {
            forEach {
                logger.info(it)
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            try {

                // Connect to the pipe
                val pipe = RandomAccessFile("""\\.\pipe\SysCon2ControlInterface""", "rw")
                logger.info("start")

//                pipe.sendRequest("sci::GetAllInstances").printResponse()
                pipe.sendRequest("laser::GetLightsources").printResponse()

//                pipe.sendRequest("sequence manager::GetSequences").printResponse()
//                pipe.sendRequest("sequence manager::SelectSequence", listOf("Sequence1.seq")).printResponse()
//                pipe.sendRequest("sequence manager::DeleteSequence", listOf("Sequence1.seq")).printResponse()
                val seqFile = """C:\Users\JanCasus\Desktop\triggeredSquares.seq"""
//                pipe.sendRequest("sequence manager::ImportSequence", listOf(seqFile,"""rde\dems3""")).printResponse()
                pipe.sendRequest("Laser::SetEmission", listOf("dummy_0","dummy_0",1)).printResponse()
                pipe.sendRequest("Laser::SetIntensity", listOf("dummy_0","dummy_0",1f)).printResponse()

//                pipe.sendRequest("uga-42::GetState").printResponse()
////                pipe.sendRequest("uga-42::UploadSequence").printResponse()
//                pipe.sendRequest("uga-42::StopSequence").printResponse()
//                pipe.sendRequest("uga-42::RunSequence", listOf(1,"auto","rising")).printResponse()
//                pipe.sendRequest("uga-42::GetState").printResponse()



                pipe.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}