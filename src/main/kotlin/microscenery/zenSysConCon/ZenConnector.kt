package microscenery.zenSysConCon

import fromScenery.lazyLogger
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.ClientSignal
import org.joml.Vector3f

class ZenConnector : MicroscopeHardwareAgent(){
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))
    
    init {
        
        this.startAgent()
    }


    //############################## called from external threads ##############################
    // to following functions are called from external threads and not from this agents thread

    fun stack(file: String){

    }

    //############################## end of called from external threads ##############################

    override fun moveStage(target: Vector3f) {
        logger.warn("moveStage Not yet implemented")
    }

    override fun onLoop() {
        logger.warn("onLoop Not yet implemented")
    }

    override fun goLive() {
        logger.warn("goLive Not yet implemented")
    }

    override fun stop() {
        logger.warn("stop Not yet implemented")
    }

    override fun snapSlice() {
        logger.warn("snapSlice Not yet implemented")
    }

    override fun shutdown() {
        logger.warn("shutdown Not yet implemented")
    }

    override fun acquireStack(meta: ClientSignal.AcquireStack) {
        logger.warn("acquireStack Not yet implemented")
    }

    override fun ablatePoints(signal: ClientSignal.AblationPoints) {
        logger.warn("ablatePoints Not yet implemented")
    }

    override fun startAcquisition() {
        logger.warn("startAcquisition Not yet implemented")
    }
}