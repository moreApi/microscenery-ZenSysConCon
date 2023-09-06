package microscenery.example

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import microscenery.MicroscenerySettings
import microscenery.setVector3f
import microscenery.signals.*
import microscenery.zenSysConCon.ZenBlueTCPConnector
import microscenery.zenSysConCon.ZenMicroscope
import microscenery.zenSysConCon.sysCon.SysConNamedPipeConnector
import org.joml.Vector3f
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class ZenMicroscopeTest {

    private var zenBlue: ZenBlueTCPConnector = Mockito.mock(ZenBlueTCPConnector::class.java)
    private var sysCon: SysConNamedPipeConnector = Mockito.mock(SysConNamedPipeConnector::class.java)
    private lateinit var zenMicroscope: ZenMicroscope

    private val imageFilePath : File = File("../models/Experiment-19.czi")

    init {
        if(!imageFilePath.exists()){
             throw IllegalStateException("Could not find ../models/Experiment-19.czi. Please download it at https://cloud.viings.de/s/fBMMxdwM47mofSY")
         }
        MicroscenerySettings.set("debug",true)
        MicroscenerySettings.setVector3f("Ablation.PrecisionUM", Vector3f(1f))
    }

    @BeforeEach
    fun prepareContext(){
        zenBlue = Mockito.mock(ZenBlueTCPConnector::class.java)
        sysCon = Mockito.mock(SysConNamedPipeConnector::class.java)
        zenMicroscope = ZenMicroscope(zenBlue,sysCon)
    }

    @AfterEach
    fun destroyContext(){
        zenMicroscope.close()
    }

    @Test
    fun startup(){
        waitForMicroscopeToFinish()
    }

    @Test
    fun readDebugStack(){
        zenMicroscope.debugStack(imageFilePath.path)
        waitForMicroscopeToFinish()
        assertStackHasBeenLoaded()
    }

    @Test
    fun getStackFromZenBlue(){
        whenever(zenBlue.getCurrentDocument()).thenReturn(imageFilePath.path)
        zenMicroscope.snapSlice()
        waitForMicroscopeToFinish()
        assertStackHasBeenLoaded()
        verify(zenBlue).runExperiment()
    }

    @Test
    fun ablationExperimentGeneration(){
        cleanUpAfterAblationExperimentGeneration() // in-case there were leftovers from previous runs
        zenMicroscope.debugStack(imageFilePath.path)
        waitForMicroscopeToFinish()
        assertStackHasBeenLoaded() //init system and hardware dimensions

        val originalTestExperiment = File("""src/test/resources/OriginalTestExperiment.czexp""")
        val testExperimentCopy = originalTestExperiment.copyTo(File("TestExperimentCopy.czexp"),true)
        val experimentPath = testExperimentCopy.absolutePath
        whenever(zenBlue.saveExperimentAndGetFilePath()).thenReturn(experimentPath)
        zenMicroscope.ablatePoints(ClientSignal.AblationPoints(listOf(
            ClientSignal.AblationPoint(Vector3f(107f, 78.7f,0f))
        )))
        waitForMicroscopeToFinish()

        val expectedCZEXP = File("""src/test/resources/GeneratedTriggered3DAblation.czexp""").readText().replace(Regex("\\s+"),"")
        val expectedSEQ = File("""src/test/resources/GeneratedTriggered3DAblation.seq""").readText().replace(Regex("\\s+"),"")

        val resultCZEXP = File("""GeneratedTriggered3DAblation.czexp""").readText().replace(Regex("\\s+"),"")
        val resultSEQ = File("""GeneratedTriggered3DAblation.seq""").readText().replace(Regex("\\s+"),"")

        // TODO fix assertEquals(expectedCZEXP,resultCZEXP)
        // todo fix assertEquals(expectedSEQ,resultSEQ)

        verify(zenBlue).importExperimentAndSetAsActive(File("""GeneratedTriggered3DAblation.czexp""").absolutePath)
        verify(zenBlue).runExperiment()
        cleanUpAfterAblationExperimentGeneration()
    }

    private fun cleanUpAfterAblationExperimentGeneration(){
        listOf("""TestExperimentCopy.czexp""",
            """TestExperimentCopy.xml""",
            """GeneratedTriggered3DAblation.czexp""",
            """GeneratedTriggered3DAblation.seq"""
        )
            .map{
                val f = File(it)
                if (f.exists()){
                    f.delete()
                }
            }
    }

    private fun waitForMicroscopeToFinish(){
        runBlocking { withTimeout(10000) {
            zenMicroscope.debugSync().acquire()
        }}
    }

    private fun assertStackHasBeenLoaded() {
        assertEquals(MicroscopeStatus::class, zenMicroscope.output.poll(500,TimeUnit.MILLISECONDS)::class)
        assertEquals(HardwareDimensions::class, zenMicroscope.output.poll(500,TimeUnit.MILLISECONDS)::class)
        val stack = zenMicroscope.output.poll() as? Stack
        assertNotNull(stack)
        assertEquals(Vector3f(107f, 78.7f, -2f), stack.from)
        assertEquals(2f, stack.to.z)
        for (i in 1..5) {
            val s = zenMicroscope.output.poll(500,TimeUnit.MILLISECONDS)
            assert(s is Slice)
            assertEquals(i, (s as Slice).Id)
        }
    }
}