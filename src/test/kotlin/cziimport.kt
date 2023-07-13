package microscenery.example

import microscenery.zenSysConCon.CZIFileWrapper
import org.lwjgl.system.MemoryUtil

fun main(args: Array<String>) {
    //val id = """C:\Users\JanCasus\Zeiss\Experiment-19.czi"""
    val id = """C:\Users\JanCasus\Zeiss\sd3\20230712_488_square_ring.czi"""

    val cziWrap = CZIFileWrapper(id)

    cziWrap.pixelDimensions.print()
    cziWrap.physicalDimensions.print()

    val bar = cziWrap.reader.openBytes(0)
    val buf = MemoryUtil.memAlloc(bar.size)
    buf.put(bar)
    buf.rewind()
}
