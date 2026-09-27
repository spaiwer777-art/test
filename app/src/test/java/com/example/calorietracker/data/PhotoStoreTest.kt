package com.example.calorietracker.data

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Imports a real JPEG the way a gallery/camera Uri would arrive and checks the stored copy. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoStoreTest {
    @Test
    fun importsAndDownscalesPhoto() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val src = File(context.cacheDir, "big.jpg")
        Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
            .let { bmp -> src.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) } }

        val out = PhotoStore.importImage(context, Uri.fromFile(src))

        assertNotNull("photo must be imported", out)
        assertTrue(out!!.exists() && out.length() > 0)
        val stored = BitmapFactory.decodeFile(out.absolutePath)
        assertEquals(1600, maxOf(stored.width, stored.height))
    }
}
