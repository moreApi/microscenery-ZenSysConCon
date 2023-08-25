package microscenery.example

import microscenery.zenSysConCon.sysCon.SysConNamedPipeConnector
import org.slf4j.LoggerFactory
import java.io.File


class SysConConStub {

    companion object {

        val logger = LoggerFactory.getLogger("PipeTest")

        private fun List<String>.printResponse() {
            forEach {
                logger.info(it)
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            try {

                // Connect to the pipe
                val sysCon = SysConNamedPipeConnector()

                logger.info("start")

                val seqFile = File("GeneratedTriggered3DAblation.seq")

                sysCon.sendRequest("sequence manager::ImportSequence", listOf(seqFile.absolutePath,"""generated""")).printResponse()
                sysCon.sendRequest("sequence manager::SelectSequence", listOf("""generated\${seqFile.name}""")).printResponse()

                sysCon.sendRequest("uga-42::UploadSequence").printResponse()
                var counter = 0
                val waitTime = 500
                var state = sysCon.sendRequest("uga-42::GetState").first()
                while (!(state.contains("Idle") && !state.contains("System not idle"))){
                    if (counter++ > 10) {
                        throw IllegalStateException("SysCon - UGA seems blocked. Aborting.")
                    }
                    logger.info("SysCon - UGA is busy. Now waiting for ${counter*waitTime}ms")
                    Thread.sleep(waitTime.toLong())
                    state = sysCon.sendRequest("uga-42::GetState").first()
                }

                sysCon.sendRequest("uga-42::RunSequence", listOf(1,"auto","rising")).printResponse() // runs, start condition, flank (ignored)

//                pipe.sendRequest("sci::GetAllInstances").printResponse()
//                sysConCon.sendRequest("laser::GetLightsources").printResponse()

//                sysConCon.sendRequest("sequence manager::GetSequences").printResponse()
//                pipe.sendRequest("sequence manager::SelectSequence", listOf("Sequence1.seq")).printResponse()
//                pipe.sendRequest("sequence manager::DeleteSequence", listOf("Sequence1.seq")).printResponse()
//                val seqFile = """C:\Users\JanCasus\Desktop\triggeredSquares.seq"""
//                sysConCon.sendRequest("sequence manager::ImportSequence", listOf(seqFile,"""rde\dems3""")).printResponse()
//                sysConCon.sendRequest("Laser::SetEmission", listOf("dummy_0","dummy_0",1)).printResponse()
//                sysConCon.sendRequest("Laser::SetIntensity", listOf("dummy_0","dummy_0",1f)).printResponse()

//                pipe.sendRequest("uga-42::GetState").printResponse()
////                pipe.sendRequest("uga-42::UploadSequence").printResponse()
//                pipe.sendRequest("uga-42::StopSequence").printResponse()
//                pipe.sendRequest("uga-42::RunSequence", listOf(1,"auto","rising")).printResponse()
//                pipe.sendRequest("uga-42::GetState").printResponse()


                sysCon.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}