package microscenery.zenSysConCon.sysCon

import org.joml.Vector2f

data class Sequence(val scanmodeFast:Boolean, val objects: List<SequenceObject>){
    override fun toString(): String = "nohash\n" +
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

abstract class SequenceObject(
    val type: String,
    val TotalTimings: String = "1",
    open val timelineInfo: TimelineInfo
) {
    fun toString(index: Int): String = "[$index]\n" +
            "type=$type\n" +
            "TotalTimings=$TotalTimings\n" +
            timelineInfo.toString(index) +
            objectSpecificString(index.toString()) +
            "[/$index]\n"

    abstract fun objectSpecificString(index: String): String
}

data class TimelineInfo(
    val TimelineIndex: String = "0",
    //val starttime: String = "0",
    val description: String = "",
    val repeats: String = "1",
    val intensity: String = "0",
    val LightsourceID: String = "210-2360_0",
    val timelinegroupid: String = "-1",
    val Stepsize: String = "1000"
) {
    fun toString(index: Int): String = "TimelineInfo0_TimelineIndex=$TimelineIndex\n" +
            "TimelineInfo0_starttime=${index*10}\n" +
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
    override fun objectSpecificString(index: String): String = "port=$port\n" +
            "behaviour=$behavior\n"
}

data class PointEntity(
    override val timelineInfo: TimelineInfo,
    val position: Vector2f,
    val layer: Int = 0
) : SequenceObject( "Entity", timelineInfo = timelineInfo) {
    override fun objectSpecificString(index: String): String = "Entity${index}_Type=0\n" +
            "Entity${index}_Filled=False\n" +
            "Entity${index}_CenterX=${position.x}\n" +
            "Entity${index}_CenterY=${position.y}\n" +
            "Entity${index}_CenterZ=$layer\n" +
            "Entity${index}_Rotation=0\n" +
            "Entity${index}_ScaleX=1\n" +
            "Entity${index}_ScaleY=1\n" +
            "Entity${index}_ScaleZ=1\n" +
            "Entity${index}_TotalIntersectionRegions=1\n" +
            "Entity${index}_VertexCount=0\n" +
            "Entity${index}_reversed=False\n" +
            "Entity${index}_IntersectionRegion0_VertexIndex=-1\n"

    override fun toString(): String {
        return super.toString()
    }
}

fun main() {
    print(Sequence(false, listOf(
        Breakpoint(TimelineInfo(),"wub"),
        PointEntity( TimelineInfo(), Vector2f(20f)),
        PointEntity( TimelineInfo(),Vector2f(4f))
    )))
}