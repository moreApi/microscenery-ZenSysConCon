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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.io.File
import kotlin.test.assertEquals


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
    fun readStack(){
        zenMicroscope.debugStack(experimentFile.path)
        waitForMicroscopeToFinish()
        assert(zenMicroscope.output.poll() is MicroscopeStatus)
        assert(zenMicroscope.output.poll() is HardwareDimensions)
        assert(zenMicroscope.output.poll() is Stack)
        for (i in 1..5){
            val s = zenMicroscope.output.poll()
            assert(s is Slice)
            assertEquals(i,(s as Slice).Id)
        }
    }

    private fun waitForMicroscopeToFinish(){
        runBlocking { withTimeout(10000) {
            zenMicroscope.debugSync().acquire()
        }}
    }
}