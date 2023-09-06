package microscenery.example

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import microscenery.MicroscenerySettings
import microscenery.lightSleepOnCondition
import microscenery.setVector3f
import microscenery.signals.ClientSignal
import microscenery.signals.Stack
import microscenery.zenSysConCon.ZenMicroscope
import org.joml.Vector3f

class HeadlessZenMicroscope {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            MicroscenerySettings.set("debug", true)
            MicroscenerySettings.setVector3f("Ablation.PrecisionUM",Vector3f(1f))
            MicroscenerySettings.set("Ablation.SysCon.LightSourceId","dummy_0_0")
            MicroscenerySettings.set("Ablation.SysCon.TriggerPort","UGA-42TTL In 1")
            val zenMicroscope = ZenMicroscope()

            zenMicroscope.snapSlice()
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