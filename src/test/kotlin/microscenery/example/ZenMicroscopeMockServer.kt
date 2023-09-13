package microscenery.example.microscenery.example

import fromScenery.SettingsEditor
import microscenery.MicroscenerySettings
import microscenery.network.RemoteMicroscopeServer
import microscenery.zenSysConCon.ZenBlueTCPConnector
import microscenery.zenSysConCon.ZenMicroscope
import microscenery.zenSysConCon.sysCon.SysConNamedPipeConnector
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.zeromq.ZContext

class ZenMicroscopeMockServer {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            SettingsEditor(MicroscenerySettings)

//        val id = """C:\Users\JanCasus\Zeiss\Experiment-19.czi"""
            val id = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring.czi"""
            val id2 = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring2.czi"""
//        val id = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring3.czi"""
            //val id = """C:\Nextcloud\Zeiss\marina-sd3-drosophila2.czi"""

            val zenBlue: ZenBlueTCPConnector = Mockito.mock(ZenBlueTCPConnector::class.java)
            val sysCon: SysConNamedPipeConnector = Mockito.mock(SysConNamedPipeConnector::class.java)
            whenever(zenBlue.getCurrentDocument()).thenReturn(id)
            val zenMicroscope = ZenMicroscope(zenBlue, sysCon)

            RemoteMicroscopeServer(zenMicroscope, ZContext())
        }
    }
}