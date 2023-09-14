package microscenery.zenSysConCon

import fromScenery.lazyLogger
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class ZenMicroscope(private val zenBlue: ZenBlueTCPConnector = ZenBlueTCPConnector(),
                    private val sysCon: SysConNamedPipeConnector = SysConNamedPipeConnector()
) : MicroscopeHardwareAgent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    // for Slices and stacks
    private var idCounter = 1
    private var currentStack: Stack? = null

    private val hardwareCommandsQueue = ArrayBlockingQueue<HardwareCommand>(5000)

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
        sysCon.sendRequest("uga-42::StopSequence")
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
            val czexpFile = zenBlue.saveExperimentAndGetFilePath()
            //val experimentFolder = File(czexpFile).parent we are not using the correct path to ?abuse? importing
            // our experiments into ZenBlue as temporary
            val experimentBaseName = "GeneratedTriggered3DAblation${SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())}"

            val layerThickness = (stack.to.z - stack.from.z) / stack.slicesCount
            val ablationLayers = splitPointsIntoLayers(hwCommand.points.map { it.position }, layerThickness)
            // todo current value is fixed to accurate scan mode points
            val timePerPointMS = MicroscenerySettings.getProperty(Settings.Ablation.DwellTimeMillis,6) + (MicroscenerySettings.getProperty(Settings.Ablation.Repetitions, 1) -1)


            val indexedAblationLayers = ablationLayers.map {
                val height = it.key
                val points = it.value

                val layerIndex = ((height - stack.from.z) / layerThickness).toInt()

                layerIndex to points
            }


            val czDoc = CzexpManipulator.parseXmlDocument(czexpFile)
            CzexpManipulator.validate(czDoc)
            CzexpManipulator.removeExperimentFeedback(czDoc)
            CzexpManipulator.addExperimentFeedbackAndSetWaitLayers(czDoc,
                indexedAblationLayers.map { it.first to (it.second.size * timePerPointMS) })
            val outputPath = "$experimentBaseName.czexp"
            CzexpManipulator.writeXmlDocument(czDoc, outputPath)
            zenBlue.importExperimentAndSetAsActive(File(outputPath).absolutePath)

            buildAndStartSysConSequence(indexedAblationLayers, experimentBaseName)

            zenBlue.runExperiment()

            // display result
            val file = zenBlue.getCurrentDocument()
            hardwareCommandsQueue.add(HardwareCommand.DisplayStack(file))

        } catch (e: IllegalStateException) {
            errorHandlingWithDebug(e)
        } catch (e: CzexpManipulator.CzexpValidationError) {
            errorHandlingWithDebug(e)
        }
    }

    private fun buildAndStartSysConSequence(
        indexedAblationLayers: List<Pair<Int, List<Vector3f>>>,
        experimentBaseName: String
    ) {
        val repeats = MicroscenerySettings.getProperty(Settings.Ablation.Repetitions, 1).toString()
        val lightSourceId = MicroscenerySettings.getProperty(Settings.Ablation.SysCon.LightSourceId, "dummyLightsource")
        val triggerPort = MicroscenerySettings.getProperty(Settings.Ablation.SysCon.TriggerPort, "dummyTriggerPort")
        val sysConSequence = Sequence(false,
            indexedAblationLayers.flatMap {
                listOf<SequenceObject>(
                    Breakpoint(TimelineInfo(LightsourceID = lightSourceId), triggerPort)
                ) +
                        it.second.map { pos ->
                            PointEntity(
                                TimelineInfo(LightsourceID = lightSourceId, repeats = repeats), pos.xy()
                            )
                        }.toList<SequenceObject>()
            }
        )
        val seqFile = File("$experimentBaseName.seq")
        seqFile.writeText(sysConSequence.toString())
        sysCon.sendRequest("sequence manager::ImportSequence", listOf(seqFile.absolutePath, """generated"""))
        sysCon.sendRequest("sequence manager::SelectSequence", listOf("""generated\${seqFile.name}"""))

        sysCon.sendRequest("uga-42::UploadSequence")
        var counter = 0
        val waitTime = 500
        while (!sysCon.sendRequest("uga-42::GetState").first().contains("Idle")) {
            logger.info("SysCon - UGA is busy. Now waiting for ${counter++ * waitTime}ms")
            Thread.sleep(waitTime.toLong())
            if (counter > 10) {
                throw IllegalStateException("SysCon - UGA seems blocked. Aborting.")
            }
        }

        sysCon.sendRequest(
            "uga-42::RunSequence",
            listOf(1, "auto", "rising")
        ) // runs, start condition, flank (ignored)
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

        hardwareDimensions = hardwareDimensions.copy(
            stageMin = meta.firstPlanePosUM.copy().apply { z *= 0.995f },
            stageMax = meta.lastPlanePosUM.copy().apply { z *= 1.005f },
            imageSize = Vector2i(meta.sizeX, meta.sizeY),
            vertexDiameter = meta.pixelSizeUM.x,
            NumericType.INT16
        )

        val stackID = 0 // TODO this is dangerous
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
