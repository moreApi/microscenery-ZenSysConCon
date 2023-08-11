package microscenery.zenSysConCon

import fromScenery.lazyLogger
import microscenery.*
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class ZenMicroscope : MicroscopeHardwareAgent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private var zenBlue = ZenBlueTCPConnector()

    // for Slices and stacks
    private var idCounter = 0
    private var currentStack: Stack? = null

    private val hardwareCommandsQueue = ArrayBlockingQueue<HardwareCommand>(5000)

    init {
        MicroscenerySettings.setVector3fIfUnset("Ablation.PrecisionUM", Vector3f(100f))

        this.startAgent()
        status = status.copy(ServerState.MANUAL)
    }


    //############################## called from external threads ##############################
    // to following functions are called from external threads and not from this agents thread

    override fun moveStage(target: Vector3f) {
        logger.warn("moveStage Not yet implemented")
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
        hardwareCommandsQueue.add(HardwareCommand.AblatePoints(signal.points))
    }

    override fun startAcquisition() {
        logger.warn("startAcquisition Not yet implemented")
    }

    fun debugStack(path: String) {
        hardwareCommandsQueue.add(HardwareCommand.DisplayStack(path))
    }

    //############################## end of called from external threads ##############################

    override fun onLoop() {
        when (val hwCommand = hardwareCommandsQueue.poll(200, TimeUnit.MILLISECONDS)) {
            is HardwareCommand.DisplayStack -> displayStack(CZIFileWrapper(hwCommand.filePath))
            HardwareCommand.CaptureStack -> try {
                zenBlue.runExperiment()
                val file = zenBlue.getCurrentDocument()
                hardwareCommandsQueue.add(HardwareCommand.DisplayStack(file))
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            is HardwareCommand.AblatePoints -> {
                // assumption: distance between layers of image == distance between ablation layers
                try {
                    val stack = currentStack ?: return
                    val czexpFile = zenBlue.saveExperimentAndGetFilePath()

                    val czDoc = parseXmlDocument(czexpFile)
                    validate(czDoc)
                    removeExperimentFeedback(czDoc)
                    val layerThickness = MicroscenerySettings.getVector3("Ablation.PrecisionUM")?.z ?: return // == slice/focus thickness
                    val ablationLayers = splitPointsIntoLayers(hwCommand.points.map { it.position },layerThickness)
                    val timePerPointUS = 50 // todo really US??

                    val waitLayers = ablationLayers.map{
                        val height = it.key
                        val points = it.value

                        val layerIndex = ((stack.from.z-height) / layerThickness).toInt()

                        layerIndex to points.size * timePerPointUS
                    }
                    addExperimentFeedbackAndSetWaitLayers(czDoc,waitLayers)
                    val outputPath = "GeneratedTriggered3DAblation.czexp"
                    writeXmlDocument(czDoc,outputPath)
                    zenBlue.importExperimentAndSetAsActive(outputPath)

                    // todo syscon part


                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                } catch (e: CzexpValidationError) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun displayStack(wrapper: CZIFileWrapper) {

        val meta = wrapper.metadata

        hardwareDimensions = hardwareDimensions.copy(
            stageMin = meta.firstPlanePosUM.copy().apply { z *= 0.995f },
            stageMax = meta.lastPlanePosUM.copy().apply { z *= 1.005f },
            imageSize = Vector2i(meta.sizeX, meta.sizeY),
            vertexDiameter = meta.pixelSizeUM.x,
            NumericType.INT16
        )

        val stackID = idCounter++
        val stackSignal = Stack(
            stackID,
            false,
            meta.firstPlanePosUM,
            meta.lastPlanePosUM,
            meta.sizeZ,
            nowMillis()
        )

        output.put(stackSignal)
        currentStack = stackSignal

        for (i in 0 until meta.sizeZ) {
            val data = wrapper.reader.openBytes(i)
            val buf = MemoryUtil.memAlloc(data.size)
            buf.put(data)
            buf.rewind()

            val sliceSignal = Slice(
                idCounter++,
                nowMillis(),
                meta.planePositionUM(i),
                data.size,
                stackID to i,
                buf
            )
            output.put(sliceSignal)
        }

    }

    private sealed class HardwareCommand {
        class DisplayStack(val filePath: String) : HardwareCommand()
        object CaptureStack : HardwareCommand()
        class AblatePoints(val points : List<ClientSignal.AblationPoint>) : HardwareCommand()
    }
}
