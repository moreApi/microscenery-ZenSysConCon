package microscenery.example

import fromScenery.SettingsEditor
import microscenery.MicroscenerySettings
import microscenery.network.RemoteMicroscopeServer
import microscenery.zenSysConCon.ZenMicroscope
import microscenery.zenSysConCon.sysCon.SysConNamedPipeConnector
import org.mockito.Mockito
import org.zeromq.ZContext

class ZenMicroscopeServer {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            SettingsEditor(MicroscenerySettings)
            val syscon = if (MicroscenerySettings.get("ZenMicroscope.mockSysCon",false)){
                Mockito.mock(SysConNamedPipeConnector::class.java)
            } else {
                SysConNamedPipeConnector()
            }
            RemoteMicroscopeServer(ZenMicroscope(sysCon = syscon), ZContext())
        }
    }
}