package microscenery.example

import microscenery.zenSysConCon.sysCon.SysConNamedPipeConnector
import org.slf4j.LoggerFactory


class SysConConTest {

    companion object{

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
                val sysConCon = SysConNamedPipeConnector()

                        logger.info("start")

//                pipe.sendRequest("sci::GetAllInstances").printResponse()
                sysConCon.sendRequest("laser::GetLightsources").printResponse()

//                pipe.sendRequest("sequence manager::GetSequences").printResponse()
//                pipe.sendRequest("sequence manager::SelectSequence", listOf("Sequence1.seq")).printResponse()
//                pipe.sendRequest("sequence manager::DeleteSequence", listOf("Sequence1.seq")).printResponse()
                val seqFile = """C:\Users\JanCasus\Desktop\triggeredSquares.seq"""
//                pipe.sendRequest("sequence manager::ImportSequence", listOf(seqFile,"""rde\dems3""")).printResponse()
                sysConCon.sendRequest("Laser::SetEmission", listOf("dummy_0","dummy_0",1)).printResponse()
                sysConCon.sendRequest("Laser::SetIntensity", listOf("dummy_0","dummy_0",1f)).printResponse()

//                pipe.sendRequest("uga-42::GetState").printResponse()
////                pipe.sendRequest("uga-42::UploadSequence").printResponse()
//                pipe.sendRequest("uga-42::StopSequence").printResponse()
//                pipe.sendRequest("uga-42::RunSequence", listOf(1,"auto","rising")).printResponse()
//                pipe.sendRequest("uga-42::GetState").printResponse()



                sysConCon.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}