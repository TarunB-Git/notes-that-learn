package dev.notechat

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** A completed job survives Activity destruction without writing a stale workspace. */
object TranscriptionStore {
    private fun file(context:Context,id:String,attachment:String):AtomicFile {
        val name=MessageDigest.getInstance("SHA-256").digest("$id:$attachment".toByteArray()).joinToString("") { "%02x".format(it) }
        return AtomicFile(File(File(context.filesDir,"transcripts").apply { mkdirs() },"$name.enc"))
    }
    fun write(context:Context,id:String,attachment:String,key:ByteArray,text:String) {
        require(text.isNotBlank() && text.length<=MAX_MESSAGE_CHARS)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,SecretKeySpec(key,"AES")); updateAAD("$id:$attachment".toByteArray()) }
        val store=file(context,id,attachment); val output=store.startWrite()
        try { output.write(cipher.iv); output.write(cipher.doFinal(text.toByteArray())); store.finishWrite(output) }
        catch(error:Exception) { store.failWrite(output); throw error }
    }
    fun read(context:Context,id:String,attachment:String,key:ByteArray):String? {
        val store=file(context,id,attachment)
        if(!store.baseFile.exists())return null
        require(store.baseFile.length() in 28..4L*MAX_MESSAGE_CHARS+28)
        val bytes=store.readFully()
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,bytes.copyOfRange(0,12))); updateAAD("$id:$attachment".toByteArray()) }
        return cipher.doFinal(bytes.copyOfRange(12,bytes.size)).toString(Charsets.UTF_8).also { require(it.isNotBlank() && it.length<=MAX_MESSAGE_CHARS) }
    }
    fun remove(context:Context,id:String,attachment:String)=file(context,id,attachment).delete()
}
