package microscenery.zenSysConCon.sysCon

import org.joml.Vector2f

data class Sequence(val scanmodeFast:Boolean, val objects: List<SequenceObject>){
    override fun toString(): String = "scanmode=${if (scanmodeFast) "Fast" else "Accurate"}\n" +
            "runs=0\n" +
            "DeviceSequenceID=0\n" +
            "invertedportcnt=0\n" +
            "AutoTriggerRulesCount=0\n" +
            "AutoTriggerRulesEnabled=False\n" +
            "timelinezoomfactor=74\n" +
            "timelineviewposx=0\n" +
            "timelineviewposy=0\n" +
            "TotalEditorGroups=0" +
            objects.joinToString("/n")
}

abstract class SequenceObject(
    open val index: Int,
    val type: String,
    val TotalTimings: String = "1",
    open val timelineInfo: TimelineInfo
) {
    override fun toString(): String = "[$index]/n" +
            "type=$type\n" +
            "TotalTimings=$TotalTimings\n" +
            timelineInfo.toString() +
            objectSpecificString() +
            "[/$index]/n"

    abstract fun objectSpecificString(): String
}

data class TimelineInfo(
    val TimelineIndex: String = "0",
    val starttime: String = "0",
    val description: String = "",
    val repeats: String = "2",
    val intensity: String = "0",
    val LightsourceID: String = "210-2360_0",
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
    override val index: Int,
    override val timelineInfo: TimelineInfo,
    val port: String,
    val behavior: String = "rise"
) : SequenceObject(index, "WaitForTTL", timelineInfo = timelineInfo) {
    override fun objectSpecificString(): String = "port=$port\n" +
            "behaviour=$behavior\n"
}

data class PointEntity(
    override val index: Int,
    override val timelineInfo: TimelineInfo,
    val position: Vector2f
) : SequenceObject(index, "Entity", timelineInfo = timelineInfo) {
    override fun objectSpecificString(): String = "Entity${index}_Type=0\n" +
            "Entity${index}_Filled=False\n" +
            "Entity${index}_CenterX=${position.x}\n" +
            "Entity${index}_CenterY=${position.y}\n" +
            "Entity${index}_CenterZ=0\n" +
            "Entity${index}_Rotation=0\n" +
            "Entity${index}_ScaleX=1\n" +
            "Entity${index}_ScaleY=1\n" +
            "Entity${index}_ScaleZ=1\n" +
            "Entity${index}_TotalIntersectionRegions=1\n" +
            "Entity${index}_VertexCount=0\n" +
            "Entity${index}_reversed=False\n" +
            "Entity${index}_IntersectionRegion0_VertexIndex=-1"
}