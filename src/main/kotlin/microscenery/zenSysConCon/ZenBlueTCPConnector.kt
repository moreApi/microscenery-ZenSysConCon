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

    private fun sendAndListenForValue(command: String): String? {
        internalSend(command)
        val resp = internalListen()
        val ok = internalListen()
        if (ok?.lowercase() != "ok") {
            throw IllegalStateException("Expected 'OK' but got: '$resp' as answer to:'$command'")
        }
        return resp
    }

    /**
     * Do not use
     */
    private fun internalSend(command: String){
        client.outputStream.write("EVAL $command".toByteArray())
    }

    /**
     * Do not use
     */
    private fun internalListen():String?{
        return if (scanner.hasNextLine()) {
            scanner.nextLine()
        } else {
            null
        }
    }

    private fun sendAndExpectOK(command: String) {
        internalSend(command)
        val resp = internalListen()
        if (resp?.lowercase() != "ok") {
            throw IllegalStateException("Expected 'OK' but got: '$resp' as answer to:'$command'")
        }
    }

    fun saveExperimentAndGetFilePath(): String {
        if (sendAndListenForValue("""Zen.Acquisition.Experiments.ActiveExperiment.Save()""")?.lowercase() != "true")
             throw IllegalStateException("Could not save ZenBlue experiment.")

        return sendAndListenForValue("""Zen.Acquisition.Experiments.ActiveExperiment.FileName""")
            ?: throw IllegalStateException("Got no ZenBlue experiment filename.")
    }

    fun runExperiment() {
        val resp = sendAndListenForValue("""Zen.Acquisition.Execute(Zen.Acquisition.Experiments.ActiveExperiment)""")
        if (resp?.trim() != "Zeiss.Micro.Scripting.ZenImage")
            throw IllegalStateException("Got '$resp' instead of expected result from running experiment.")
    }

    fun importExperimentAndSetAsActive(experimentPath: String){
        sendAndExpectOK("""exp = Zen.Acquisition.Experiments.GetByFileName("$experimentPath")""")
        sendAndExpectOK("""Zen.Acquisition.Experiments.ActiveExperiment = exp""")
    }

    fun getCurrentDocument(): String {
        return sendAndListenForValue("""Zen.Application.ActiveDocument.FileName""")
            ?: throw IllegalStateException("Got no filename.")
    }
}