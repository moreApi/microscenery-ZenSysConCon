package microscenery.example

import loci.common.DebugTools
import microscenery.network.RemoteMicroscopeServer
import microscenery.zenSysConCon.CZIFileWrapper
import microscenery.zenSysConCon.ZenConnector
import org.zeromq.ZContext

fun main(args: Array<String>) {
    //val id = """C:\Users\JanCasus\Zeiss\Experiment-19.czi"""
    val id = """C:\Users\JanCasus\Zeiss\sd3\20230712_488_square_ring.czi"""

    DebugTools.setRootLevel("OFF")
    val cziWrap = CZIFileWrapper(id)
    val zenConnector = ZenConnector()
    val server = RemoteMicroscopeServer(zenConnector, ZContext())
    zenConnector.stack(cziWrap)
    println("lol")
    //val bar = cziWrap.reader.openBytes(0)
    //val buf = MemoryUtil.memAlloc(bar.size)
    //buf.put(bar)
    //buf.rewind()
}
