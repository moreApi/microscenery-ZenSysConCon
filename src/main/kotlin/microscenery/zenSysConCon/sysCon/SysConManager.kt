package microscenery.zenSysConCon.sysCon

import fromScenery.lazyLogger
import microscenery.MicroscenerySettings
import microscenery.Settings
import java.io.File

class SysConManager(val sysCon: SysConNamedPipeConnector) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    init {
        MicroscenerySettings.setIfUnset(Settings.Ablation.Rapp.ExperimentsPath,
            """C:\ProgramData\ROE\SysCon 2\2.3.0\profiles\Jan\Sequence Manager\Experiments""")
    }

    fun uploadSequence(experimentBaseName: String, sysConSequence: Sequence){
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
    }

    private class Entity() : LinkedHashMap<String, String>() {
        val id: Int
            get() {
                return this["id"]?.toInt() ?: -1
            }
        val startTime: Int
            get() {
                return this["TimelineInfo0_starttime"]?.toInt() ?: -1
            }
        val layer: Int
            get() {
                return this["Entity1_CenterZ"]?.toInt() ?: -1
            }
    }

    private class ParsedSequence(val meta: Map<String,String>, val entities: List<Entity>)

    fun getTimings(experimentBaseName: String){
        val path = MicroscenerySettings.get<String>(Settings.Ablation.Rapp.ExperimentsPath) + """\generated\$experimentBaseName.seq"""
        val seqFile = File(path)
        if (!seqFile.exists()){
            throw IllegalArgumentException( "Could not find seq file at $path. Please set ${Settings.Ablation.Rapp.ExperimentsPath} accordingly.")
        }
        val seq = parseSequence(seqFile.readText())
        val perLayer = seq.entities.fold(mutableMapOf<Int,MutableList<Entity>>()){ acc, e ->
            val list = acc.getOrDefault(e.layer, mutableListOf())
            list += e
            acc[e.layer] = list
            acc
        }
        perLayer.forEach { (_, u) -> u.sortBy { it.startTime } }

        //todo continue

    }

    private fun parseSequence(text:String): ParsedSequence {
        val iter = text.lineSequence().iterator()
        // skipp first line
        iter.next()
        val meta = mutableMapOf<String,String>()
        val entities = mutableListOf<Entity>()
        var currentEntity: Entity? = null
        while (iter.hasNext()){
            val line = iter.next()
            when {
                line.startsWith("[/") -> {
                    entities += currentEntity ?: throw Error("there should be an entity")
                    currentEntity = null
                }
                line.startsWith("[") -> {
                    currentEntity = Entity()
                    currentEntity["id"] = line.replace("[","").replace("]","")
                }
                else -> {
                    val spl = line.split("=")
                    if (currentEntity != null){
                        currentEntity[spl.first()] = spl.getOrElse(1) { "" }
                    } else {
                        meta[spl.first()] = spl.getOrElse(1) { "" }
                    }
                }
            }
        }
        return ParsedSequence(meta,entities)
    }
}