package microscenery.example

import fromScenery.SettingsEditor
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.network.RemoteMicroscopeServer
import microscenery.zenSysConCon.ZenMicroscope
import microscenery.zenSysConCon.sysCon.SysConConnection
import org.mockito.Mockito
import org.zeromq.ZContext

class ZenMicroscopeServer {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            SettingsEditor(MicroscenerySettings)
            val syscon = if (MicroscenerySettings.get(Settings.ZenMicroscope.MockSysCon,false)){
                Mockito.mock(SysConConnection::class.java)
            } else {
                SysConConnection()
            }
            RemoteMicroscopeServer(ZenMicroscope(sysCon = syscon), ZContext())
        }
    }
}