package microscenery.zenSysConCon

import org.joml.Vector3f

/**
 * Put [points] into bins/layers with size/thickness of [precision] along the Z axis
 * with minimum and maximum being the highest and lowest z Value of [points].
 * The maximum amount of layers is (maxZ - minZ) / [precision] rounded up.
 * The middle of the first layer it at the position of the lowest point. This assures that the at least the
 * first point is hit as close as possible. For all following it can`t be guaranteed anyway without making
 * assumptions about the distribution of the points along z.
 *
 * For [precision] == 0 each point gets its own layer and only exact matches in z share a layer.
 *
 * @return sorted hashmap of layers containing points with the keys being the middle of the layer.
 */
fun splitPointsIntoLayers(points: List<Vector3f>, precision: Float = 0f): Map<Float, List<Vector3f>> {
    if (precision == 0f){
        val layers = mutableMapOf<Float,List<Vector3f>>()
        points.forEach {
            val layer = layers.getOrDefault(it.z, emptyList())
            layers[it.z] = layer + it
        }
        return layers.toSortedMap()
    }

    val min = points.minOf { it.z } - precision * 0.5f
    val layers = mutableMapOf<Int,List<Vector3f>>()
    // to following values are not used in the algorithm but might help a human to understand it
    //val max = points.maxOf { it.z }
    //val range = max - min
    //val countLayers = ceil(range / precision).toInt()
    // precision = layerThickness = step size

    points.forEach {
        val index = ((it.z - min) / precision).toInt()
        val layer = layers.getOrDefault(index , emptyList())
        layers[index] = layer + it
    }

    return layers.map {
        val (index, p) = it
        index*precision + min + precision * 0.5f to p
    }.toMap().toSortedMap()
}
