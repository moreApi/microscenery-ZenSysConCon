package microscenery.example

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import microscenery.signals.HardwareDimensions
import microscenery.signals.MicroscopeStatus
import microscenery.signals.Slice
import microscenery.signals.Stack
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class ZenMicroscopeTest {

    private var zenBlue: ZenBlueTCPConnector = Mockito.mock(ZenBlueTCPConnector::class.java)
    private var sysCon: SysConNamedPipeConnector = Mockito.mock(SysConNamedPipeConnector::class.java)
    private lateinit var zenMicroscope: ZenMicroscope

    private val experimentFile : File = File("../models/Experiment-19.czi")

    init {
        if(!experimentFile.exists()){
             throw IllegalStateException("Could not find ../models/Experiment-19.czi. Please download it at https://cloud.viings.de/s/fBMMxdwM47mofSY")
         }
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
        zenMicroscope.debugStack(experimentFile.path)
        waitForMicroscopeToFinish()
        assertStackHasBeenLoaded()
    }

    @Test
    fun getStackFromZenBlue(){
        whenever(zenBlue.getCurrentDocument()).thenReturn(experimentFile.path)
        zenMicroscope.snapSlice()
        waitForMicroscopeToFinish()
        assertStackHasBeenLoaded()
        verify(zenBlue).runExperiment()
    }

    private fun waitForMicroscopeToFinish(){
        runBlocking { withTimeout(10000) {
            zenMicroscope.debugSync().acquire()
        }}
    }

    private fun assertStackHasBeenLoaded() {
        assert(zenMicroscope.output.poll() is MicroscopeStatus)
        assert(zenMicroscope.output.poll() is HardwareDimensions)
        val stack = zenMicroscope.output.poll() as? Stack
        assertNotNull(stack)
        assertEquals(Vector3f(107f, 78.7f, -2f), stack.from)
        assertEquals(2f, stack.to.z)
        for (i in 1..5) {
            val s = zenMicroscope.output.poll()
            assert(s is Slice)
            assertEquals(i, (s as Slice).Id)
        }
    }
}