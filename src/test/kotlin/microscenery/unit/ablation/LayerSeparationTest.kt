package microscenery.unit.ablation

import microscenery.zenSysConCon.splitPointsIntoLayers
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class LayerSeparationTest {

    @Test
    fun layerSeparation0Precision(){
        val pointsInput = listOf(Vector3f(1f),Vector3f(0f,0f,1f),Vector3f(1.1f),Vector3f(-11f),Vector3f(0f))
        val expectedOutput = mapOf(
            -11f to listOf( Vector3f(-11f)),
            0f to listOf( Vector3f(0f)),
            1f to listOf( Vector3f(1f),Vector3f(0f,0f,1f)),
            1.1f to listOf( Vector3f(1.1f)),
        )
        val result = splitPointsIntoLayers(pointsInput,0f)

        assertEquals(expectedOutput.size,result.size, "same amount of bins")

        expectedOutput.forEach {
            val (layer, points) = it
            assertContains(result.keys, layer, "same keys")
            val resultLayer = result[layer]!!
            points.forEach { point ->
                assertContains(resultLayer, point, "same bin content")
            }
        }
    }


    @Test
    fun layerSeparationRegularPrecision(){
        val pointsInput = listOf(
            Vector3f(0f),
            Vector3f(10f),
            Vector3f(0f,0f,2.5f),
            Vector3f(0f,0f,2.3f),
            Vector3f(0f,0f,2.1f),
            Vector3f(-10f),
            Vector3f(2.5f)
        )
        val expectedOutput = mapOf(
            -10f to listOf( Vector3f(-10f)),
            0f to listOf( Vector3f(0f)),
            2f to listOf(Vector3f(0f,0f,2.1f)),
            2.5f to listOf( Vector3f(2.5f),Vector3f(0f,0f,2.3f),Vector3f(0f,0f,2.5f)),
            10f to listOf( Vector3f(10f)),
        )
        val result = splitPointsIntoLayers(pointsInput,0.5f)

        assertEquals(expectedOutput.size,result.size, "same amount of bins")

        expectedOutput.forEach {
            val (layer, points) = it
            assertContains(result.keys, layer, "same keys")
            val resultLayer = result[layer]!!
            points.forEach { point ->
                assertContains(resultLayer, point, "same bin content")
            }
        }
    }

    @Test
    fun layerSeparationNonDiviseableRange(){
        val pointsInput = listOf(
            Vector3f(0f),
            Vector3f(1f),
            Vector3f(1.6f)
        )
        val expectedOutput = mapOf(
            0f to listOf( Vector3f(0f)),
            1f to listOf( Vector3f(1f)),
            2f to listOf(Vector3f(1.6f))
        )
        val result = splitPointsIntoLayers(pointsInput,1f)

        assertEquals(expectedOutput.size,result.size, "same amount of bins")

        expectedOutput.forEach {
            val (layer, points) = it
            assertContains(result.keys, layer, "same keys")
            val resultLayer = result[layer]!!
            points.forEach { point ->
                assertContains(resultLayer, point, "same bin content")
            }
        }
    }


    @Test
    fun layerSeparationMorePointsThanLayers(){
        val pointsInput = listOf(
            Vector3f(0f),
            Vector3f(10f),
            Vector3f(-7.2f),
            Vector3f(0f,0f,-2.5f),
            Vector3f(0f,0f,2.3f),
            Vector3f(0f,0f,2.1f),
            Vector3f(-10f),
            Vector3f(2.5f)
        )
        val expectedOutput = mapOf(
            -10f to listOf( Vector3f(-10f)),
            -5f to listOf( Vector3f(-7.2f)),
            0f to listOf(Vector3f(0f,0f,-2.5f),
                Vector3f(0f), Vector3f(0f,0f,2.1f),Vector3f(0f,0f,2.3f)),
            5f to listOf( Vector3f(2.5f)),
            10f to listOf( Vector3f(10f)),
        )
        val result = splitPointsIntoLayers(pointsInput,5f)

        assertEquals(expectedOutput.size,result.size, "same amount of bins")

        expectedOutput.forEach {
            val (layer, points) = it
            assertContains(result.keys, layer, "same keys")
            val resultLayer = result[layer]!!
            points.forEach { point ->
                assertContains(resultLayer, point, "same bin content")
            }
        }
    }

}