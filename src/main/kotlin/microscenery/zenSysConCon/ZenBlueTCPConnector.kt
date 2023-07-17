package microscenery.zenSysConCon

import java.net.Socket
import java.util.*

class ZenBlueTCPConnector(host: String = "localhost", port: Int = 52757) {
    val client = Socket(host, port)
    val scanner = Scanner(client.inputStream)

    init {
        while (scanner.hasNextLine()) {
            println(scanner.nextLine())
            break
        }
    }

    fun close() {
        client.close()
    }

    private fun sendAndListen(commands: String): String? {
        client.outputStream.write("EVAL $commands".toByteArray())
        return if (scanner.hasNextLine()) {
            scanner.nextLine()
        } else {
            null
        }
    }

    private fun sendAndExpectOK(command: String) {
        val resp = sendAndListen(command)
        if (resp != "OK") {
            throw IllegalStateException("Expected 'OK' but got: $resp")
        }
    }

    fun saveExperimentAndGetFilePath(saveAs: String = "GeneratedTriggered3DAblation"): String {
        sendAndExpectOK("""Zen.Acquisition.Experiments.ActiveExperiment.SaveAs("$saveAs",False)""")
        return sendAndListen("""Zen.Acquisition.Experiments.ActiveExperiment.FileName""")
            ?: throw IllegalStateException("Got no filename.")
    }

    fun runExperiment() {
        sendAndExpectOK("""Zen.Acquisition.Execute(Zen.Acquisition.Experiments.ActiveExperiment.)""")
    }

    fun importExperimentAndSetAsActive(experimentPath: String){
        sendAndExpectOK("""exp = Zen.Acquisition.Experiments.GetByFileName("$experimentPath")""")
        sendAndExpectOK("""Zen.Acquisition.Experiments.ActiveExperiment = exp""")
    }

    fun getCurrentDocument(): String {
        return sendAndListen("""Zen.Application.ActiveDocument.FileName""")
            ?: throw IllegalStateException("Got no filename.")
    }
}