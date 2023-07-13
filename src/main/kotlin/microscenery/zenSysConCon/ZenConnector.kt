package microscenery.zenSysConCon

import fromScenery.lazyLogger
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.nowMillis
import microscenery.signals.*
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil

class ZenConnector : MicroscopeHardwareAgent(){
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    // for Slices and stacks
    private var idCounter = 0

    init {
        
        //this.startAgent()
        status = status.copy(ServerState.MANUAL)
    }


    //############################## called from external threads ##############################
    // to following functions are called from external threads and not from this agents thread

    fun stack(wrapper: CZIFileWrapper){

        //wrapper.meta.pos

        val meta = wrapper.metadata

        hardwareDimensions = hardwareDimensions.copy(
            stageMin = meta.firstPlanePosUM.mul(0.95f),
            stageMax = meta.lastPlanePosUM.mul(1.05f) + (Vector3f(meta.sizeX.toFloat(),meta.sizeY.toFloat(),0f)*meta.pixelSizeUM),
            imageSize = Vector2i(meta.sizeX,meta.sizeY),
            vertexDiameter = meta.pixelSizeUM.x,
            NumericType.INT16
        )

        val stackID = idCounter++
        val stackSignal = Stack(stackID,
            false,
            meta.firstPlanePosUM,
            meta.lastPlanePosUM,
            meta.sizeZ,
            nowMillis()
        )

        output.put(stackSignal)

        for (i in 0 until meta.sizeZ){
            val data = wrapper.reader.openBytes(i)
            val buf = MemoryUtil.memAlloc(data.size)
            buf.put(data)
            buf.rewind()

            val sliceSignal = Slice(idCounter++,
                nowMillis(),
                meta.planePositionUM(i),
                data.size,
                stackID to i,
                buf
            )
            output.put(sliceSignal)
        }

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