package dev.notechat

import android.content.Context
import android.graphics.Color
import android.util.TypedValue

interface UiColorSource { val uiColors: UiColors }

data class UiColors(val dark: Boolean, val notes: Boolean = false, val theme: String = "classic") {
    private val tint = ThemePalette.colors[theme]
    private fun mix(base: Int, amount: Float) = tint?.let { androidx.core.graphics.ColorUtils.blendARGB(base,it,amount) } ?: base
    val surface = if(notes && dark)Color.rgb(24,24,27) else if (dark) Color.rgb(17,27,33) else Color.WHITE
    val paper = mix(if(notes)if(dark)Color.rgb(12,12,14) else Color.rgb(246,246,248) else if (dark) Color.rgb(11,20,26) else Color.rgb(239,234,226),if(dark).12f else .06f)
    val ink = if (dark) Color.rgb(233,237,239) else Color.rgb(17,27,33)
    val muted = if (dark) Color.rgb(166,180,189) else Color.rgb(102,119,129)
    val accent = tint?.let { if(dark)androidx.core.graphics.ColorUtils.blendARGB(it,Color.WHITE,.38f) else it } ?: if(notes)if(dark)Color.rgb(222,96,51) else Color.rgb(173,68,32) else if (dark) Color.rgb(37,211,166) else Color.rgb(0,128,105)
    val onAccent = if(androidx.core.graphics.ColorUtils.calculateContrast(Color.WHITE,accent)>=4.5)Color.WHITE else Color.rgb(17,27,33)
    val bubble = mix(if (dark) Color.rgb(0,92,75) else Color.rgb(217,253,211),if(dark).45f else .12f)
    val inset = if (dark) Color.rgb(32,44,51) else Color.rgb(244,245,246)
    val quote = mix(if (dark) Color.rgb(0, 70, 58) else Color.rgb(201,236,196),.16f)
    val selected = mix(if (dark) Color.rgb(42, 70, 75) else Color.rgb(193,223,217),.18f)
    val divider = if (dark) Color.rgb(42,55,63) else Color.rgb(235,238,239)
    companion object {
        fun of(context: Context): UiColors {
            var source: Context?=context
            while(source!=null) {
                if(source is UiColorSource)return source.uiColors
                val base=(source as? android.content.ContextWrapper)?.baseContext
                if(base===source)break
                source=base
            }
            val value = TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorBackground, value, true)
            return UiColors(Color.red(value.data) < 100)
        }
    }
}

/** Stable keys are stored in backups; unknown keys fall back to the classic palette. */
object ThemePalette {
    val colors = linkedMapOf("forest" to Color.rgb(0,107,87), "ocean" to Color.rgb(21,92,148),
        "plum" to Color.rgb(117,67,147), "clay" to Color.rgb(167,70,32), "rose" to Color.rgb(162,55,89))
    val keys = listOf("classic") + colors.keys
    fun title(key: String) = key.replaceFirstChar { it.uppercase() }
}
