package microscenery.zenSysConCon

import fromScenery.lazyLogger
import loci.common.services.ServiceFactory
import loci.formats.IFormatReader
import loci.formats.ImageReader
import loci.formats.meta.IMetadata
import loci.formats.services.OMEXMLService
import ome.units.UNITS
import ome.units.quantity.Length
import ome.units.quantity.Time
import org.joml.Vector4f

class CZIFileWrapper(val path: String) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val pixelDimensions: PixelDimensions
    val physicalDimensions: PhysicalDimensions
    val reader: IFormatReader

    init {
        var series = 1

        // create OME-XML metadata store
        val factory = ServiceFactory()
        val service = factory.getInstance(OMEXMLService::class.java)
        val meta: IMetadata = service.createOMEXMLMetadata()

        // create format reader
        reader = ImageReader()
        reader.metadataStore = meta

        // initialize file
        logger.info("Initializing $path")
        reader.setId(path)

        pixelDimensions = PixelDimensions(reader)

        val seriesCount = reader.seriesCount
        if (series < seriesCount) reader.series = series
        series = reader.series
        println("\tImage series = $series of $seriesCount")
        physicalDimensions = readPhysicalDimensions(meta, series)
    }

    data class PixelDimensions(val X: Int, val Y: Int, val Z: Int, val C: Int, val T: Int, val imageCount: Int, val bytesPerPixel: Int) {
        constructor(reader: IFormatReader) :
                this(
                    reader.sizeX,
                    reader.sizeY,
                    reader.sizeZ,
                    reader.sizeC,
                    reader.sizeT,
                    reader.imageCount,
                    (reader.bitsPerPixel+7)/8
                )

        fun print() {
            println("Pixel dimensions:")
            println("\tWidth = $X")
            println("\tHeight = $Y")
            println("\tFocal planes = $Z")
            println("\tChannels = $C")
            println("\tTimepoints = $T")
            println("\tTotal planes = $imageCount")
            println("\tBytes per Pixel = $bytesPerPixel")
        }
    }

    private fun readPhysicalDimensions(meta: IMetadata, series: Int): PhysicalDimensions {
        return PhysicalDimensions(
            meta.getPixelsPhysicalSizeX(series),
            meta.getPixelsPhysicalSizeY(series),
            meta.getPixelsPhysicalSizeZ(series),
            meta.getPixelsTimeIncrement(series)
        )
    }

    data class PhysicalDimensions(val sizeX: Length, val sizeY: Length, val sizeZ: Length, val sizeTimeStep: Time?) {
        fun toVector(): Vector4f = Vector4f(
            sizeX.value(UNITS.MICROMETER).toFloat(),
            sizeY.value(UNITS.MICROMETER).toFloat(),
            sizeZ.value(UNITS.MICROMETER).toFloat(),
            sizeTimeStep?.value(UNITS.MILLISECOND)?.toFloat() ?: 0f
        )

        fun print() {
            println("Physical dimensions:")
            println("\tX spacing = ${sizeX.value()} ${sizeX.unit().symbol}")
            println("\tY spacing = ${sizeY.value()} ${sizeY.unit().symbol}")
            println("\tZ spacing = ${sizeZ.value()} ${sizeZ.unit().symbol}")
            println("\tTime increment = ${sizeTimeStep?.value(UNITS.SECOND)?.toDouble().toString()} seconds")
        }
    }

}