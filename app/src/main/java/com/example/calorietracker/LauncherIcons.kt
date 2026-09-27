package com.example.calorietracker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/** Launcher icon styles; each maps to an <activity-alias> in the manifest. */
enum class AppIcon(
    val label: String,
    val alias: String,
    val res: Int,
    /** Layers of the adaptive icon, for drawing previews without the launcher. */
    val foreground: Int,
    val background: Long,
    val enabledByDefault: Boolean = false
) {
    CLASSIC("Классика", ".LauncherClassic", R.mipmap.ic_launcher, R.mipmap.ic_launcher_foreground, 0xFFEEF2DC, enabledByDefault = true),
    DARK("На тёмном", ".LauncherDark", R.mipmap.ic_launcher_dark, R.mipmap.ic_launcher_fg_framed, 0xFF1F2B22),
    ROBOT("Крупно", ".LauncherRobot", R.mipmap.ic_launcher_robot, R.mipmap.ic_launcher_fg_robot, 0xFFEEF2DC),
    GREEN("На зелёном", ".LauncherGreen", R.mipmap.ic_launcher_green, R.mipmap.ic_launcher_fg_framed, 0xFF3E7B45)
}

object LauncherIcons {
    private fun component(context: Context, icon: AppIcon) = ComponentName(context.packageName, context.packageName + icon.alias)

    fun current(context: Context): AppIcon = AppIcon.entries.firstOrNull { icon ->
        when (context.packageManager.getComponentEnabledSetting(component(context, icon))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon.enabledByDefault
            else -> false
        }
    } ?: AppIcon.CLASSIC

    /** Swaps the app-drawer icon. The new alias is enabled first so the app never has zero launcher entries. */
    fun apply(context: Context, icon: AppIcon) {
        val pm = context.packageManager
        pm.setComponentEnabledSetting(component(context, icon), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        AppIcon.entries.filter { it != icon }.forEach {
            pm.setComponentEnabledSetting(component(context, it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }

    fun canPinShortcut(context: Context) = ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    /**
     * Asks the launcher to add a home-screen shortcut with any name and picture; this is the only way
     * Android allows an app to show a user-chosen name on the home screen.
     */
    fun pinShortcut(context: Context, name: String, icon: AppIcon, photo: Bitmap?): Boolean {
        if (!canPinShortcut(context)) return false
        val intent = Intent(Intent.ACTION_MAIN).setClass(context, MainActivity::class.java)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val shortcut = ShortcutInfoCompat.Builder(context, "home-" + System.currentTimeMillis())
            .setShortLabel(name.ifBlank { context.getString(R.string.app_name) }.take(25))
            .setIcon(photo?.let { IconCompat.createWithAdaptiveBitmap(it) } ?: IconCompat.createWithResource(context, icon.res))
            .setIntent(intent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    /** Square, centre-cropped picture for an adaptive shortcut icon (the launcher masks the edges). */
    fun loadSquare(context: Context, uri: Uri, size: Int = 432): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= size) sample *= 2
        val raw = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val degrees = context.contentResolver.openInputStream(uri)?.use {
            androidx.exifinterface.media.ExifInterface(it).rotationDegrees
        } ?: 0
        val upright = if (degrees == 0) raw else Bitmap.createBitmap(
            raw, 0, 0, raw.width, raw.height, android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }, true
        )
        val side = minOf(upright.width, upright.height)
        val square = Bitmap.createBitmap(upright, (upright.width - side) / 2, (upright.height - side) / 2, side, side)
        return Bitmap.createScaledBitmap(square, size, size, true)
    }
}
