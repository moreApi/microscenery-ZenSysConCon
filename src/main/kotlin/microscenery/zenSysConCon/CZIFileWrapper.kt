package microscenery.zenSysConCon

import fromScenery.lazyLogger
import loci.common.services.ServiceFactory
import loci.formats.IFormatReader
import loci.formats.ImageReader
import loci.formats.meta.IMetadata
import loci.formats.services.OMEXMLService
import microscenery.toReadableString
import ome.units.UNITS
import org.joml.Vector3f

class CZIFileWrapper(val path: String) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val metadata: MetaData
    val reader: IFormatReader

    init {
        // create OME-XML metadata store
        val factory = ServiceFactory()
        val service = factory.getInstance(OMEXMLService::class.java)
        val meta: IMetadata
        meta = service.createOMEXMLMetadata()

        // create format reader
        reader = ImageReader()
        reader.metadataStore = meta

        // initialize file
        logger.info("Initializing $path")
        reader.setId(path)

        this.metadata = MetaData(reader, meta)
        logger.info(this.metadata.toString())
    }

    @Suppress("MemberVisibilityCanBePrivate")
    data class MetaData(val reader: IFormatReader, val meta: IMetadata, var series: Int = 1) {
        init {
            val seriesCount = reader.seriesCount
            if (series < seriesCount) reader.series = series
            series = reader.series
        }

        val sizeX = reader.sizeX
        val sizeY = reader.sizeY
        val sizeZ = reader.sizeZ
        val sizeC = reader.sizeC
        val sizeT = reader.sizeT
        val imageCount = reader.imageCount
        val bytesPerPixel = (reader.bitsPerPixel + 7) / 8

        override fun toString(): String =
            "Pixel dimensions:\n" +
            "\tWidth = $sizeX\n" +
            "\tHeight = $sizeY\n" +
            "\tFocal planes = $sizeZ\n" +
            "\tChannels = $sizeC\n" +
            "\tTimepoints = $sizeT\n" +
            "\tTime step size = $timeStepSize\n" +
            "\tTotal planes = $imageCount\n" +
            "\tBytes per Pixel = $bytesPerPixel\n" +
            "\tPixel size in um: ${pixelSizeUM.toReadableString()}\n" +
            "\tFirst plane pos in um: ${firstPlanePosUM.toReadableString()}\n" +
            "\tLast plane pos in um: ${lastPlanePosUM.toReadableString()}\n"

        val pixelSizeUM: Vector3f = Vector3f(
            meta.getPixelsPhysicalSizeX(series).value(UNITS.MICROMETER).toFloat(),
            meta.getPixelsPhysicalSizeY(series).value(UNITS.MICROMETER).toFloat(),
            meta.getPixelsPhysicalSizeZ(series).value(UNITS.MICROMETER).toFloat(),
        )

        val timeStepSize = meta.getPixelsTimeIncrement(series)?.value(UNITS.MILLISECOND)?.toFloat() ?: 0f

        fun planePositionUM(plane: Int): Vector3f = Vector3f(
            meta.getPlanePositionX(0, plane).value(UNITS.MICROMETER).toFloat(),
            meta.getPlanePositionY(0, plane).value(UNITS.MICROMETER).toFloat(),
            meta.getPlanePositionZ(0, plane).value(UNITS.MICROMETER).toFloat(),
        )

        val firstPlanePosUM = planePositionUM(0)
        val lastPlanePosUM = planePositionUM(sizeZ-1)
    }
}