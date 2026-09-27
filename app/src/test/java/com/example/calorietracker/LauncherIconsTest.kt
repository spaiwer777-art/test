package com.example.calorietracker

import android.app.Application
import android.content.Context
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherIconsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Aliases explicitly enabled, or enabled by the manifest default. */
    private fun launcherEntries(): List<AppIcon> = AppIcon.entries.filter {
        when (context.packageManager.getComponentEnabledSetting(ComponentName(context.packageName, context.packageName + it.alias))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> it.enabledByDefault
            else -> false
        }
    }

    @Test
    fun classicIsTheOnlyLauncherEntryByDefault() {
        assertEquals(AppIcon.CLASSIC, LauncherIcons.current(context))
        assertEquals(listOf(AppIcon.CLASSIC), launcherEntries())
    }

    @Test
    fun switchingKeepsExactlyOneLauncherEntry() {
        LauncherIcons.apply(context, AppIcon.DARK)
        assertEquals(AppIcon.DARK, LauncherIcons.current(context))
        assertEquals(listOf(AppIcon.DARK), launcherEntries())
        LauncherIcons.apply(context, AppIcon.CLASSIC)
        assertEquals(AppIcon.CLASSIC, LauncherIcons.current(context))
        assertEquals(listOf(AppIcon.CLASSIC), launcherEntries())
    }

    @Test
    fun shortcutPictureIsSquare() {
        val src = File(context.cacheDir, "wide.png")
        Bitmap.createBitmap(1200, 600, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
            .let { bmp -> src.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        val square = LauncherIcons.loadSquare(context, Uri.fromFile(src))
        assertNotNull(square)
        assertEquals(432, square!!.width)
        assertEquals(432, square.height)
    }
}
