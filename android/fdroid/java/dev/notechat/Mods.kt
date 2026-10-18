package dev.notechat

import android.content.Context
import java.io.File

/** Google SDKs and models are absent from this build. */
object Mods {
    const val available = false
    private fun unavailable(): Nothing = error("Translation and OCR are unavailable in the F-Droid build")
    fun languages(): List<String> = emptyList()
    fun downloaded(): Set<String> = emptySet()
    fun download(language: String): Unit = unavailable()
    fun remove(language: String): Unit = unavailable()
    fun translate(context: Context, text: String, target: String): String = unavailable()
    fun installOcr(context: Context): Unit = unavailable()
    fun ocr(file: File): String = unavailable()
}
