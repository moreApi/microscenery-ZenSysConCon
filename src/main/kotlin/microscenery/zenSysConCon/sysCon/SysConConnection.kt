package microscenery.zenSysConCon.sysCon

import fromScenery.lazyLogger
import java.io.File

class SysConConnection (val pipe: SysConNamedPipeConnector = SysConNamedPipeConnector()) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    fun uploadSequence(experimentBaseName: String, sysConSequence: Sequence){
        val seqFile = File("$experimentBaseName.seq")
        seqFile.writeText(sysConSequence.toString())
        pipe.sendRequest("sequence manager::ImportSequence", listOf(seqFile.absolutePath, """generated"""))
        pipe.sendRequest("sequence manager::SelectSequence", listOf("""generated\${seqFile.name}"""))

        pipe.sendRequest("uga-42::UploadSequence")
        var counter = 0
        val waitTime = 500
        while (!pipe.sendRequest("uga-42::GetState").first().contains("Idle")) {
            logger.info("SysCon - UGA is busy. Now waiting for ${counter++ * waitTime}ms")
            Thread.sleep(waitTime.toLong())
            if (counter > 10) {
                throw IllegalStateException("SysCon - UGA seems blocked. Aborting.")
            }
        }
    }

    fun close() {
        pipe.close()
    }

    fun stop() {
        pipe.sendRequest("uga-42::StopSequence")
        //todo: close shutter
    }

    fun startSequence() {
        pipe.sendRequest(
            "uga-42::RunSequence",
            listOf(1, "auto", "rising")
        ) // runs, start condition, flank (ignored)
    }

}