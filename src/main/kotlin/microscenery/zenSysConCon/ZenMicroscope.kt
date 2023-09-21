package microscenery.zenSysConCon

import fromScenery.lazyLogger
import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.xy
import kotlinx.coroutines.sync.Semaphore
import microscenery.*
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import microscenery.signals.Stack
import microscenery.zenSysConCon.sysCon.*
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import org.w3c.dom.Document
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class ZenMicroscope(private val zenBlue: ZenBlueTCPConnector = ZenBlueTCPConnector(),
                    private val sysCon: SysConConnection = SysConConnection()
) : MicroscopeHardwareAgent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private var sliceIdCounter = 0
    private var stackIdCounter = 0
    private var currentStack: Stack? = null

    private val hardwareCommandsQueue = ArrayBlockingQueue<HardwareCommand>(5000)

    private val experimentBaseNameBase = "Generated"
    private var exposure = 0f

    init {
        MicroscenerySettings.setVector3fIfUnset(Settings.Ablation.PrecisionUM, Vector3f(1f))

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
        sysCon.stop()
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
                buildAndUploadImagingCzExp(experimentBaseNameBase+"Imaging"+ SimpleDateFormat("yyyyMMdd-HHmmss").format(Date()))
                zenBlue.runExperiment()
                val file = zenBlue.getCurrentDocument()
                hardwareCommandsQueue.add(HardwareCommand.DisplayStack(file))
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            is HardwareCommand.AblatePoints -> {
                // assumption: distance between layers of image == distance between ablation layers
                handleAblatePointsCommand(hwCommand)
            }
            is HardwareCommand.DebugSync -> {
                hwCommand.lock.release()
            }
        }
    }

    private fun handleAblatePointsCommand(hwCommand: HardwareCommand.AblatePoints) {
        try {
            val stack = currentStack ?: return
            val experimentBaseName = experimentBaseNameBase+"Triggered3DAblation"+ SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())

            val layerThickness = (stack.to.z - stack.from.z) / stack.slicesCount
            val ablationLayers = splitPointsIntoLayers(hwCommand.points.map { it.position }, layerThickness)
            // todo current value is fixed to accurate scan mode points ( 5ms galvo movement + 0.05 per "repetition"
            //todo: uncomment this val timePerPointMS = 5 + (hwCommand.points.first().dwellTime / 1000f).roundToInt()
            val timePerPointMS = 5 + hwCommand.points.first().dwellTime.toInt()

            val indexedAblationLayers = ablationLayers.map {
                val height = it.key
                val points = it.value

                val layerIndex = ((height - stack.from.z) / layerThickness).toInt()

                layerIndex to points
            }

            buildAndUploadAblationCzExp(indexedAblationLayers, timePerPointMS, experimentBaseName)
            buildAndStartSysConSequence(indexedAblationLayers, experimentBaseName,hwCommand.points.first().dwellTime)

            zenBlue.runExperiment()

            hardwareCommandsQueue.add(HardwareCommand.CaptureStack)

        } catch (e: IllegalStateException) {
            errorHandlingWithDebug(e)
        } catch (e: CzexpManipulator.CzexpValidationError) {
            errorHandlingWithDebug(e)
        }
    }

    private fun buildAndUploadAblationCzExp(
        indexedAblationLayers: List<Pair<Int, List<Vector3f>>>,
        timePerPointMS: Int,
        experimentBaseName: String
    ) {
        val czexpFile = zenBlue.saveExperimentAndGetFilePath()
        //val experimentFolder = File(czexpFile).parent
        // we are not using the correct path to ?abuse? importing
        // our experiments into ZenBlue as temporary
        val czDoc = CzexpManipulator.parseXmlDocument(czexpFile)
        extractExposure(czexpFile,czDoc)
        CzexpManipulator.validate(czDoc)
        if (!CzexpManipulator.setAllExposure(czDoc,1f)) logger.warn("Could not set exposure to 1 for ablation.")
        CzexpManipulator.removeExperimentFeedback(czDoc)
        CzexpManipulator.addExperimentFeedbackAndSetWaitLayers(czDoc,
            indexedAblationLayers.map { it.first to (it.second.size * timePerPointMS) })
        val outputPath = "$experimentBaseName.czexp"
        CzexpManipulator.writeXmlDocument(czDoc, outputPath)
        zenBlue.importExperimentAndSetAsActive(File(outputPath).absolutePath)
    }

    private fun buildAndUploadImagingCzExp(
        experimentBaseName: String
    ) {
        val czexpFile = zenBlue.saveExperimentAndGetFilePath()
        //val experimentFolder = File(czexpFile).parent
        // we are not using the correct path to ?abuse? importing
        // our experiments into ZenBlue as temporary
        val czDoc = CzexpManipulator.parseXmlDocument(czexpFile)
        extractExposure(czexpFile,czDoc)
        if (exposure == 0f) {
            logger.error("Exposure is 0. This means exposure could not be extracted." )
        } else {
            CzexpManipulator.setAllExposure(czDoc, exposure)
        }
        CzexpManipulator.validate(czDoc)
        CzexpManipulator.removeExperimentFeedback(czDoc)
        val outputPath = "$experimentBaseName.czexp"
        CzexpManipulator.writeXmlDocument(czDoc, outputPath)
        zenBlue.importExperimentAndSetAsActive(File(outputPath).absolutePath)
    }

    private fun extractExposure(czexpFile: String, parsedXML: Document){
        if (czexpFile.startsWith(experimentBaseNameBase)) return
        CzexpManipulator.getExposure(parsedXML)?.let { exposure = it }
    }

    private fun buildAndStartSysConSequence(
        indexedAblationLayers: List<Pair<Int, List<Vector3f>>>,
        experimentBaseName: String,
        dwellTimeUS: Long
    ) {
        val lightSourceId = MicroscenerySettings.getProperty(Settings.Ablation.SysCon.LightSourceId, "dummyLightsource")
        val triggerPort = MicroscenerySettings.getProperty(Settings.Ablation.SysCon.TriggerPort, "dummyTriggerPort")
        val sysConSequence = Sequence(
            MicroscenerySettings.getProperty(Settings.Ablation.SysCon.ScanModeFast,false),
            indexedAblationLayers.flatMap {
                listOf<SequenceObject>(
                    Breakpoint(TimelineInfo(LightsourceID = lightSourceId), triggerPort)
                ) +
                        it.second.map { pos ->
                            val stack = currentStack ?: return
                            val imagePos = (pos - ((stack.from.copy() + stack.to).mul(0.5f))) / hardwareDimensions.vertexDiameter
                            imagePos.x += hardwareDimensions.imageSize.x /2
                            imagePos.y += hardwareDimensions.imageSize.y /2

                            PointEntity(
                                // 50 us per repeat
                                TimelineInfo(LightsourceID = lightSourceId, repeats = dwellTimeUS.toString()), imagePos.xy()
                            )
                        }.toList<SequenceObject>()
            }
        )

        sysCon.uploadSequence(experimentBaseName,sysConSequence)

        sysCon.startSequence()
    }

    private fun errorHandlingWithDebug(e: Throwable) {
        if (MicroscenerySettings.get("debug", false))
            throw e
        else {
            logger.error("Could not ablate because:")
            e.printStackTrace()
        }
    }

    private fun displayStack(wrapper: CZIFileWrapper) {

        val meta = wrapper.metadata

        val hwDmMin = meta.firstPlanePosUM.copy().apply { z *= 0.9995f }
        val hwDmMax = meta.lastPlanePosUM.copy().apply { z *= 1.0005f }

        if (hwDmMin != hardwareDimensions.stageMin || hwDmMax != hardwareDimensions.stageMax){
            // we got a new stack
            stackIdCounter++
            hardwareDimensions = hardwareDimensions.copy(
                stageMin = hwDmMin,
                stageMax = hwDmMax,
                imageSize = Vector2i(meta.sizeX, meta.sizeY),
                vertexDiameter = meta.pixelSizeUM.x,
                NumericType.INT16
            )
        }

        val stackID = stackIdCounter
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
                sliceIdCounter++,
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
