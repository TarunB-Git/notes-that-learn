package dev.notechat

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.graphics.ColorUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class AppearanceTest {
    @Test fun paletteSurvivesBackupAndOldWorkspacesKeepTheClassicTheme() {
        val state=ChatState(emptyList(),preferences=Preferences(colorTheme="plum"))
        assertEquals("plum",StateJson.decode(StateJson.encode(BackupArchive.portable(state))).preferences.colorTheme)
        val old=org.json.JSONObject(StateJson.encode(state).toString(Charsets.UTF_8))
        old.getJSONObject("preferences").remove("colorTheme")
        assertEquals("classic",StateJson.decode(old.toString().toByteArray()).preferences.colorTheme)
        assertEquals(UiColors(false).accent,UiColors(false,theme="unknown").accent)
        val activity=org.robolectric.Robolectric.buildActivity(MainActivity::class.java).get()
        MainActivity::class.java.getDeclaredField("shown").apply { isAccessible=true }.set(activity,state)
        assertEquals(UiColors(false,theme="plum").accent,UiColors.of(activity).accent)
        assertEquals(UiColors.of(activity),UiColors.of(android.view.ContextThemeWrapper(activity,R.style.AppTheme)))
    }
    @Test fun allPalettesHaveReadableActionTextInBothModesAndAppearances() {
        for(key in ThemePalette.keys)for(dark in listOf(false,true))for(notes in listOf(false,true)) {
            val colors=UiColors(dark,notes,key)
            assertTrue("$key dark=$dark notes=$notes",ColorUtils.calculateContrast(colors.onAccent,colors.accent)>=4.5)
            assertTrue(ColorUtils.calculateContrast(colors.ink,colors.paper)>=4.5)
            assertTrue("Accent $key dark=$dark notes=$notes",ColorUtils.calculateContrast(colors.accent,colors.surface)>=4.5)
        }
    }
    @Test @Config(sdk=[30,35]) fun iconSwitchLeavesExactlyOneLauncherAndKeepsShareActivityEnabled() {
        val context=RuntimeEnvironment.getApplication()
        for(key in ThemePalette.keys.reversed()+"classic") {
            LauncherIcons.select(context,key)
            assertEquals(key,LauncherIcons.selected(context))
            val launchers=context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName),0)
            assertEquals(1,launchers.size)
            assertEquals("dev.notechat.Launcher${ThemePalette.title(key)}",launchers.single().activityInfo.name)
            assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,context.packageManager.getComponentEnabledSetting(ComponentName(context,MainActivity::class.java)))
        }
        assertThrows(IllegalArgumentException::class.java) { LauncherIcons.select(context,"unknown") }
    }
}
