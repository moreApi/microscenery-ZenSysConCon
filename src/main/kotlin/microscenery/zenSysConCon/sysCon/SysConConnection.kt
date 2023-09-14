package microscenery.zenSysConCon.sysCon

import fromScenery.lazyLogger
import microscenery.MicroscenerySettings
import microscenery.Settings
import java.io.File

class SysConConnection(private val pipe: SysConNamedPipeConnector = SysConNamedPipeConnector()) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    init {
        fun validateLightSource() {
            val lightSources = pipe.sendRequest("laser::GetLightsources")
            val selectedLightSource = getLightSourceId()
            if (!lightSources.any { it.contains(selectedLightSource) }) {
                logger.warn("Cant find $selectedLightSource in light sources. Please adjust setting ${Settings.Ablation.SysCon.LightSourceId} before performing ablation.")
                logger.info("available options are:")
                lightSources.forEach { logger.info(it) }
            }
        }

        validateLightSource()
        MicroscenerySettings.addUpdateRoutine(Settings.Ablation.SysCon.LightSourceId) { validateLightSource() }
        MicroscenerySettings.addUpdateRoutine(Settings.Ablation.LaserPower){
            setLaserPower(MicroscenerySettings.get(Settings.Ablation.LaserPower,0f))
        }
    }

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

    fun setShutter(open: Boolean){
        pipe.sendRequest("laser:SetShutter", listOf(getLightSourceId(),open))
    }

    fun setLaserPower(power: Float){
        pipe.sendRequest("Laser::SetIntensity", listOf(getLightSourceId(),getLightSourceDescription(),power))
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

    private fun getLightSourceId() = MicroscenerySettings.getProperty(
        Settings.Ablation.SysCon.LightSourceId,
        "dummyLightsource"
    )

    private fun getLightSourceDescription(): String {
        return "todo"
    }

}