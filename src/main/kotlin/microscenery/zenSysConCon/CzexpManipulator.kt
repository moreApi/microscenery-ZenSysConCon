package microscenery.zenSysConCon

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.Text
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun parseXmlDocument(xmlFilePath: String): Document {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val document = builder.parse(xmlFilePath)

    // Filter out line breaks
    filterLineBreaks(document)

    document.documentElement.normalize()
    return document
}

private fun filterLineBreaks(node: Node) {
    if (node.nodeType == Node.TEXT_NODE) {
        val textNode = node as Text
        val filteredText = textNode.nodeValue.replace("[\\r\\n]".toRegex(), "")
        if (filteredText.isBlank()) {
            node.parentNode.removeChild(node)
        }
    } else {
        val childNodes = node.childNodes
        for (i in childNodes.length - 1 downTo 0) {
            val childNode = childNodes.item(i)
            filterLineBreaks(childNode)
        }
    }
}

fun NodeList.asList() : List<Node> {
    if (this.length == 0) return emptyList()
    return (0 until this.length).map { this.item(it) }
}

fun Node?.isActivated()  = this?.attributes?.getNamedItem("IsActivated")?.nodeValue == "true"



fun writeXmlDocument(document: Document, outputFilePath: String) {
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()

    // Configure to use explicit closing tags
    transformer.setOutputProperty(OutputKeys.METHOD, "xml")

    // indentation
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

    val source = DOMSource(document)
    val result = StreamResult(outputFilePath)
    transformer.transform(source, result)
}

/**
 * validate (no tiles, no timeseries, no experiment designer activated) only one experiemnt
 */
fun validate(document: Document){
    // only one experiment
    val blocks =
        document.documentElement.getElementsByTagName("ExperimentBlocks").item(0)
            ?.childNodes?.asList()?.filter { it.isActivated() }
    if (blocks?.size != 1){
        throw CzexpValidationError("Experiment needs to contain exactly only one activated experiment block")
    }

    val acquisitionBlock = blocks.first()
    if (acquisitionBlock.nodeName != "AcquisitionBlock"){
        throw CzexpValidationError("Experiment needs to contain an AcquisitionBlock block")
    }

    val zStackSetup = acquisitionBlock.childNodes.asList().firstOrNull { it.nodeName == "ZStackSetup"}
        ?.childNodes?.asList()?.firstOrNull { it.nodeName == "ZStackSetup"}
    if (zStackSetup.isActivated()){
        throw CzexpValidationError("AcquisitionBlock block needs active ZStack")
    }

    // TODO MAYBE: Tiles, region are on the same level as zStack and should be validated to being off
}

class CzexpValidationError(reason: String) : Error(reason)

fun removeExperimentFeedback(document: Document){
    val ef = document.getElementsByTagName("ExperimentFeedback").item(0) ?:return
    document.firstChild.removeChild(ef)
}

/**
 * @param waitLayer List<layerIndex to waitTimeMS>
 */
fun addExperimentFeedbackAndSetWaitLayers(document: Document, waitLayer: List<Pair<Int,Int>>){
    val experimentFeedbackFilePath = object {}.javaClass.classLoader.getResource("ExperimentFeedback.xml")?.file
        ?: throw Error("ExperimentFeedback.xml missing")
    val experimentFeedback = parseXmlDocument(experimentFeedbackFilePath)

    val scriptCode = waitLayer.joinToString("\n") { (layer, waitTime) ->
        """
            if ZenService.Experiment.CurrentZSliceIndex == $layer:
                ZenService.HardwareActions.SetTriggerDigitalOut7(True)
                System.Threading.Thread.Sleep($waitTime)
                ZenService.HardwareActions.SetTriggerDigitalOut7(False)
        """.trimIndent()
    }

    experimentFeedback.getElementsByTagName("LoopScript").item(0).textContent = scriptCode

    // the root element is of type document. the first child is the first actual element.
    document.firstChild.appendChild(document.adoptNode(experimentFeedback.firstChild))
}

fun main() {
    val xmlFilePath = """C:\Nextcloud\Zeiss\20230419_Test3_stack - Copy.czexp"""
    val outputFilePath = "output.xml"

    // Parse the XML document
    val document = parseXmlDocument(xmlFilePath)
    validate(document)
    removeExperimentFeedback(document)
    addExperimentFeedbackAndSetWaitLayers(document, listOf(1 to 200, 3 to 3000))
//    val insertBook = parseXmlDocument(xmlFilePath2)

    // Modify the XML document
//    modifyXmlDocument2(document,insertBook.firstChild)

    // Write the modified XML document to a file
    writeXmlDocument(document, outputFilePath)

    println("XML document processing completed.")
}