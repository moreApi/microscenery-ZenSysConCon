package microscenery.zenSysConCon.sysCon

import org.joml.Vector2f

data class Sequence(val scanmodeFast:Boolean, val objects: List<SequenceObject>){
    override fun toString(): String {

        return "nohash\n" +
                "scanmode=${if (scanmodeFast) "Fast" else "Accurate"}\n" +
                "runs=0\n" +
                "DeviceSequenceID=0\n" +
                "invertedportcnt=0\n" +
                "AutoTriggerRulesCount=0\n" +
                "AutoTriggerRulesEnabled=False\n" +
                "timelinezoomfactor=74\n" +
                "timelineviewposx=0\n" +
                "timelineviewposy=0\n" +
                "TotalEditorGroups=0\n" +
                objects.reversed().mapIndexed { index, sequenceObject -> sequenceObject.toString(index) }.joinToString("")
    }
}

abstract class SequenceObject(
    val type: String,
    val TotalTimings: String = "1",
    open val timelineInfo: TimelineInfo
) {
    fun toString(index: Int): String = "[$index]\n" +
            "type=$type\n" +
            "TotalTimings=$TotalTimings\n" +
            timelineInfo.toString() +
            objectSpecificString(index.toString()) +
            "[/$index]\n"

    abstract fun objectSpecificString(index: String): String
}

data class TimelineInfo(
    val TimelineIndex: String = "0",
    var starttime: String = "0",
    val description: String = "",
    var repeats: String = "1",
    val intensity: String = "0",
    val LightsourceID: String = "dummy_0_0",
    val timelinegroupid: String = "-1",
    val Stepsize: String = "1000"
) {
    override fun toString(): String = "TimelineInfo0_TimelineIndex=$TimelineIndex\n" +
            "TimelineInfo0_starttime=$starttime\n" +
            "TimelineInfo0_description=$description\n" +
            "TimelineInfo0_repeats=$repeats\n" +
            "TimelineInfo0_intensity=$intensity\n" +
            "TimelineInfo0_LightsourceID=$LightsourceID\n" +
            "TimelineInfo0_timelinegroupid=$timelinegroupid\n" +
            "TimelineInfo0_Stepsize=$Stepsize\n"
}

data class Breakpoint(
    override val timelineInfo: TimelineInfo,
    val port: String,
    val behavior: String = "rise"
) : SequenceObject( "WaitForTTL", timelineInfo = timelineInfo) {
    init {
        timelineInfo.repeats = "0"
    }

    override fun objectSpecificString(index: String): String = "port=$port\n" +
            "behaviour=$behavior\n"
}

data class PointEntity(
    override val timelineInfo: TimelineInfo,
    val position: Vector2f
) : SequenceObject( "Entity", timelineInfo = timelineInfo) {
    override fun objectSpecificString(index: String): String = "Entity${index}_Type=0\n" +
            "Entity${index}_Filled=False\n" +
            "Entity${index}_CenterX=${position.x}\n" +
            "Entity${index}_CenterY=${position.y}\n" +
            "Entity${index}_CenterZ=0\n" +
            "Entity${index}_Rotation=0\n" +
            "Entity${index}_ScaleX=1\n" +
            "Entity${index}_ScaleY=1\n" +
            "Entity${index}_ScaleZ=1\n" +
            "Entity${index}_VertexCount=0\n" +
            "Entity${index}_reversed=False\n"

    override fun toString(): String {
        return super.toString()
    }
}

fun main() {
//    val f = File("""asd.seq""")
//
//    f.writeText(
//        Sequence(false, (0..10).flatMap {
//            listOf(
//                Breakpoint(TimelineInfo(), "UGA-42TTL In 1"),
//                PointEntity(TimelineInfo(), Vector2f(it*30f)),
//                PointEntity(TimelineInfo(), Vector2f(it*30+10f)),
//            )
//        }).toString()
//    )
    SysConConnection().uploadSequence("asdd",Sequence(false, (0..10).flatMap {
        listOf(
            Breakpoint(TimelineInfo(), "UGA-42TTL In 1"),
            PointEntity(TimelineInfo(), Vector2f(it*30f)),
            PointEntity(TimelineInfo(), Vector2f(it*30+10f)),
        )
        })
    )

    println("done")
}