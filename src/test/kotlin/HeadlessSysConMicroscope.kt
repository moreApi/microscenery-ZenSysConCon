package microscenery.example

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import microscenery.MicroscenerySettings
import microscenery.lightSleepOnCondition
import microscenery.setVector3
import microscenery.signals.ClientSignal
import microscenery.signals.Stack
import microscenery.zenSysConCon.ZenBlueTCPConnector
import microscenery.zenSysConCon.ZenMicroscope
import org.joml.Vector3f
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import java.io.File

class HeadlessSysConMicroscope {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            MicroscenerySettings.set("debug", true)
            MicroscenerySettings.setVector3("Ablation.PrecisionUM",Vector3f(1f))
            MicroscenerySettings.set("Ablation.SysCon.LightSourceId","dummy_0_0")
            MicroscenerySettings.set("Ablation.SysCon.TriggerPort","UGA-42TTL In 1")

            val imageFilePath = File("models/Experiment-19.czi")

            val zenBlue = Mockito.mock(ZenBlueTCPConnector::class.java)
            val originalTestExperiment = File("""zenSysConCon/src/test/resources/OriginalTestExperiment.czexp""")
            val testExperimentCopy = originalTestExperiment.copyTo(File("TestExperimentCopy.czexp"),true)
            val experimentPath = testExperimentCopy.absolutePath
            whenever(zenBlue.saveExperimentAndGetFilePath()).thenReturn(experimentPath)

            val zenMicroscope = ZenMicroscope(zenBlue = zenBlue)

            // --- init done ---

            zenMicroscope.debugStack(imageFilePath.path)

            lightSleepOnCondition { zenMicroscope.output.poll()?.let {
                return@let it is Stack
            } ?: false }

            zenMicroscope.ablatePoints(ClientSignal.AblationPoints(listOf(
                ClientSignal.AblationPoint(Vector3f(0f,0f,0f)),
                ClientSignal.AblationPoint(Vector3f(0f,0f,2f)),
                ClientSignal.AblationPoint(Vector3f(0f,0f,2f))
            )))
//            while (true){
//                val signale = zenMicroscope.output.take()
//                println(signale)
//            }
            runBlocking {
                launch {
                    zenMicroscope.debugSync().acquire()
                }
            }



        }
    }
}