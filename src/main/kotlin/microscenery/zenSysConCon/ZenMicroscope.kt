package microscenery.zenSysConCon

import fromScenery.lazyLogger
import fromScenery.utils.extensions.xy
import kotlinx.coroutines.sync.Semaphore
import microscenery.*
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import microscenery.zenSysConCon.sysCon.*
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class ZenMicroscope(private val zenBlue: ZenBlueTCPConnector = ZenBlueTCPConnector(),
                    private val sysCon: SysConNamedPipeConnector = SysConNamedPipeConnector()
) : MicroscopeHardwareAgent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    // for Slices and stacks
    private var idCounter = 0
    private var currentStack: Stack? = null

    private val hardwareCommandsQueue = ArrayBlockingQueue<HardwareCommand>(5000)

    init {
        MicroscenerySettings.setVector3fIfUnset("Ablation.PrecisionUM", Vector3f(100f))

        this.startAgent()
        status = status.copy(ServerState.MANUAL)
    }

    override fun onClose(){
        zenBlue.close()
        sysCon.close()
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
        hardwareCommandsQueue.add(HardwareCommand.CaptureStack)
    }

    override fun shutdown() {
        logger.warn("shutdown Not yet implemented")
    }

    override fun acquireStack(meta: ClientSignal.AcquireStack) {
        hardwareCommandsQueue.add(HardwareCommand.CaptureStack)
    }

    override fun ablatePoints(signal: ClientSignal.AblationPoints) {
        hardwareCommandsQueue.add(HardwareCommand.AblatePoints(signal.points))
    }

    override fun startAcquisition() {
        hardwareCommandsQueue.add(HardwareCommand.CaptureStack)
    }

    fun debugStack(path: String) {
        hardwareCommandsQueue.add(HardwareCommand.DisplayStack(path))
    }

    fun debugSync(): Semaphore {
        val lock = Semaphore(1, 1)
        hardwareCommandsQueue.add(HardwareCommand.DebugSync(lock))
        return lock
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
                    val layerThickness =
                        MicroscenerySettings.getVector3("Ablation.PrecisionUM")?.z ?: return // == slice/focus thickness
                    val ablationLayers = splitPointsIntoLayers(hwCommand.points.map { it.position }, layerThickness)
                    val timePerPointUS = 50 // todo really US??

                    val indexedAblationLayers = ablationLayers.map {
                        val height = it.key
                        val points = it.value

                        val layerIndex = ((stack.from.z - height) / layerThickness).toInt()

                        layerIndex to points
                    }
                    addExperimentFeedbackAndSetWaitLayers(czDoc,
                        indexedAblationLayers.map { it.first to it.second.size * timePerPointUS })
                    val outputPath = "GeneratedTriggered3DAblation.czexp"
                    writeXmlDocument(czDoc, outputPath)
                    zenBlue.importExperimentAndSetAsActive(outputPath)

                    val repeats = "1"
                    val lightSourceId = "dummyLightsource"
                    val triggerPort = "dummyTriggerPort"
                    val sysConSequence = Sequence(true,
                        indexedAblationLayers.flatMap {
                            listOf<SequenceObject>(
                                Breakpoint(TimelineInfo(LightsourceID = lightSourceId), triggerPort)
                            ) +
                            it.second.map {
                                PointEntity(
                                    TimelineInfo(LightsourceID = lightSourceId, repeats = repeats), it.xy()
                                )
                            }.toList<SequenceObject>()
                        }
                    )
                    val seqFile = File("GeneratedTriggered3DAblation.seq")
                    seqFile.writeText(sysConSequence.toString())
                    sysCon.sendRequest("sequence manager::ImportSequence", listOf(seqFile.absolutePath,"""generated\GeneratedTriggered3DAblation"""))

                    sysCon.sendRequest("uga-42::UploadSequence")

                    sysCon.sendRequest("uga-42::RunSequence", listOf(1,"auto","rising")) // runs, start condition, flank (ignored)
                    zenBlue.runExperiment()


                } catch (e: IllegalStateException) {
                    errorHandlingWithDebug(e)
                } catch (e: CzexpValidationError) {
                    errorHandlingWithDebug(e)
                }
            }
            is HardwareCommand.DebugSync -> {
                hwCommand.lock.release()
            }
        }
    }

    private fun errorHandlingWithDebug(e: Throwable) {
        if (MicroscenerySettings.get("debug", false))
            throw e
        else {
            System.err.println("Could not ablate because:")
            e.printStackTrace()
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
        class AblatePoints(val points: List<ClientSignal.AblationPoint>) : HardwareCommand()
        class DebugSync(val lock: Semaphore) : HardwareCommand()
    }

}
