package microscenery.example

import loci.common.DebugTools
import microscenery.network.RemoteMicroscopeServer
import microscenery.zenSysConCon.CZIFileWrapper
import microscenery.zenSysConCon.ZenMicroscope
import org.zeromq.ZContext

fun main(args: Array<String>) {
    //val id = """C:\Users\JanCasus\Zeiss\Experiment-19.czi"""
    val id = """C:\Users\JanCasus\Zeiss\sd3\20230712_488_square_ring3.czi"""

    DebugTools.setRootLevel("OFF")
    val cziWrap = CZIFileWrapper(id)
    val zenMicroscope = ZenMicroscope()
    val server = RemoteMicroscopeServer(zenMicroscope, ZContext())
    //zenConnector.stack(cziWrap)
    println("lol")
    //val bar = cziWrap.reader.openBytes(0)
    //val buf = MemoryUtil.memAlloc(bar.size)
    //buf.put(bar)
    //buf.rewind()
}
