package dev.notechat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** User-started local file processing; only the selected audio and a job key remain unlocked. */
class TranscriptionService:Service() {
    private class Job(val id:String,val attachment:String,val input:InputStream,val key:ByteArray) {
        val cancelled=AtomicBoolean(false)
    }
    companion object {
        const val CHANGED="dev.notechat.TRANSCRIPTION_CHANGED"
        private const val CHANNEL="transcription"
        private const val NOTIFICATION=73
        private val guard=Any()
        private var job:Job?=null
        private val failures=linkedMapOf<String,String>()
        internal var recognize:(Context,File,()->Boolean)->String = { context,file,cancelled->VoiceTranscriber.transcribe(context,file,cancelled) }
        fun running(id:String)=synchronized(guard) { job?.id==id }
        fun active()=synchronized(guard) { job!=null }
        fun cancel(id:String)=synchronized(guard) { job?.takeIf { it.id==id }?.cancelled?.set(true) }
        fun failure(id:String)=synchronized(guard) { failures[id] }
        fun enqueue(context:Context,id:String,attachment:String,input:InputStream,key:ByteArray):Boolean {
            synchronized(guard) {
                if(job!=null) { input.close(); key.fill(0); return false }
                job=Job(id,attachment,input,key); failures.remove(id)
            }
            try { context.startForegroundService(Intent(context,TranscriptionService::class.java)); return true }
            catch(error:Exception) {
                synchronized(guard) { job?.input?.close(); job?.key?.fill(0); job=null }
                throw error
            }
        }
    }
    private val executor=Executors.newSingleThreadExecutor()
    private var started=false
    private var owned:Job?=null
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action=="cancel") { synchronized(guard) { job?.cancelled?.set(true) }; if(!started)stopSelf(); return START_NOT_STICKY }
        val current=synchronized(guard) { job } ?: run { stopSelf(); return START_NOT_STICKY }
        if(started)return START_NOT_STICKY
        started=true; owned=current
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"Audio transcription",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,73,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cancel=PendingIntent.getService(this,74,Intent(this,TranscriptionService::class.java).setAction("cancel"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_note).setContentTitle("Transcribing recording")
            .setContentText("On this phone · you can leave the app").setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE).setProgress(0,0,true).addAction(Notification.Action.Builder(null,"Cancel",cancel).build()).build()
        try { startForeground(NOTIFICATION,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) }
        catch(error:Exception) { finish(current,error); stopSelf(); return START_NOT_STICKY }
        executor.execute {
            val audio=File(cacheDir,"transcribe-audio-${java.util.UUID.randomUUID()}")
            val wake=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"NoteChat:transcription")
            try {
                wake.acquire(16*60*1000L)
                current.input.use { input->audio.outputStream().use { output->input.copyTo(output) } }
                val text=recognize(this,audio) { current.cancelled.get() || Thread.currentThread().isInterrupted }
                check(!current.cancelled.get()) { "Transcription cancelled" }
                TranscriptionStore.write(this,current.id,current.attachment,current.key,text)
                finish(current,null)
            } catch(error:Exception) { finish(current,error) }
            finally { audio.delete(); if(wake.isHeld)wake.release(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
        }
        sendBroadcast(Intent(CHANGED).setPackage(packageName))
        return START_NOT_STICKY
    }
    private fun finish(current:Job,error:Exception?) {
        runCatching { current.input.close() }; current.key.fill(0)
        synchronized(guard) {
            if(error!=null)failures[current.id]=if(current.cancelled.get())"Transcription cancelled. Tap to retry." else "Could not transcribe this recording. Tap to retry."
            while(failures.size>64)failures.remove(failures.keys.first())

        }
        sendBroadcast(Intent(CHANGED).setPackage(packageName))
    }
    override fun onTimeout(startId:Int,fgsType:Int) { synchronized(guard) { job?.cancelled?.set(true) }; stopSelf() }
    override fun onDestroy() {
        owned?.cancelled?.set(true)
        synchronized(guard) { if(job===owned)job=null }
        executor.shutdownNow()
        sendBroadcast(Intent(CHANGED).setPackage(packageName))
        super.onDestroy()
    }
}
