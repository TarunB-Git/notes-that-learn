package dev.notechat

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class TranscriptionServiceTest {
    @Test fun foregroundJobSurvivesItsCallingActivityAndSavesOnlyOneResult() {
        val context=RuntimeEnvironment.getApplication()
        val entered=CountDownLatch(1); val release=CountDownLatch(1); val calls=AtomicInteger()
        val original=TranscriptionService.recognize
        val key=ByteArray(32) { 9 }
        val controller=Robolectric.buildService(TranscriptionService::class.java).create()
        val activity=Robolectric.buildActivity(android.app.Activity::class.java).create().start().resume()
        TranscriptionService.recognize={ _,_,cancelled ->
            calls.incrementAndGet(); entered.countDown()
            check(release.await(5,TimeUnit.SECONDS)); check(!cancelled()); "Remember the appointment"
        }
        try {
            assertTrue(TranscriptionService.enqueue(activity.get(),"background","audio",byteArrayOf(1,2).inputStream(),key.copyOf()))
            controller.startCommand(0,1)
            assertTrue(entered.await(5,TimeUnit.SECONDS))
            activity.pause().stop().destroy()
            assertTrue(TranscriptionService.running("background"))
            assertFalse(TranscriptionService.enqueue(context,"background","audio",byteArrayOf(3).inputStream(),key.copyOf()))
            release.countDown()
            val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
            var saved:String?=null
            while(saved==null && System.nanoTime()<deadline) {
                saved=runCatching { TranscriptionStore.read(context,"background","audio",key) }.getOrNull()
                if(saved==null)Thread.sleep(10)
            }
            assertEquals("Remember the appointment",saved)
            assertEquals(1,calls.get())
        } finally {
            release.countDown(); controller.destroy(); TranscriptionService.recognize=original
            TranscriptionStore.remove(context,"background","audio")
        }
    }
}
