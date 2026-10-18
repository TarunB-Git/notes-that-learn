package dev.notechat

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Launcher choice is device-local; it never disables the real activity or share target. */
object LauncherIcons {
    private fun component(context: Context, key: String) = ComponentName(context.packageName,
        "dev.notechat.Launcher${ThemePalette.title(key)}")

    fun selected(context: Context): String = ThemePalette.keys.firstOrNull { key ->
        val state=context.packageManager.getComponentEnabledSetting(component(context,key))
        state==PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            key=="classic" && state==PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    } ?: "classic"

    fun select(context: Context, key: String) {
        require(key in ThemePalette.keys)
        val manager=context.packageManager
        val changes=ThemePalette.keys.map { value -> Triple(component(context,value),
            if(value==key)PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP) }
        if(Build.VERSION.SDK_INT>=33)manager.setComponentEnabledSettings(changes.map {
            PackageManager.ComponentEnabledSetting(it.first,it.second,it.third)
        }) else {
            // Enable first so that older Android versions always retain a launchable entry.
            changes.sortedBy { it.second!=PackageManager.COMPONENT_ENABLED_STATE_ENABLED }.forEach {
                manager.setComponentEnabledSetting(it.first,it.second,it.third)
            }
        }
    }
}
