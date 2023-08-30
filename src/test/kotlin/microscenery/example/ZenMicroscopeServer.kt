package microscenery.example

import fromScenery.SettingsEditor
import microscenery.MicroscenerySettings
import microscenery.network.RemoteMicroscopeServer
import microscenery.zenSysConCon.ZenMicroscope
import org.zeromq.ZContext

class ZenMicroscopeServer {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            SettingsEditor(MicroscenerySettings)
            RemoteMicroscopeServer(ZenMicroscope(), ZContext())
        }
    }
}