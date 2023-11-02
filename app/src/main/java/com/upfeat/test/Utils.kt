package com.upfeat.test

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * This is a Util file, containing useful functions used in the app's functions (file creation/storage)
 */

// Storing bitmap file, while taking into consideration device's OS,
// and not occupying the main thread to not cause lag
fun store(bm: Bitmap, fileName: String?) {
    CoroutineScope(Dispatchers.IO).launch{
        val dir = Environment.getExternalStorageDirectory().toString() + "/UpfeatTest/"

        val folder = File(
            dir
        )

        if (!folder.exists()) {
            folder.mkdir()
        }

        val file = File(folder, fileName)
        try{
            file.createNewFile()
        }catch (e : Exception){
            //if for any cause the above approach causes Exception (caused IOException while testing),
            // use alternative approach, which will work if above won't as per my extensive testing.
            storeAlternatively(bm, fileName)
        }

        try {
            val fOut = FileOutputStream(file)
            bm.compress(Bitmap.CompressFormat.PNG, 85, fOut)
            fOut.flush()
            fOut.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun storeAlternatively(bm: Bitmap, fileName: String?) {
    val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + "/UpfeatTest/" + fileName
        )
    } else {
        File(Environment.getExternalStorageDirectory().toString() + "/UpfeatTest/" + fileName)
    }

    if (!dir.exists()) dir.parentFile?.mkdirs()
    try {
        val fOut = FileOutputStream(dir)
        bm.compress(Bitmap.CompressFormat.PNG, 85, fOut)
        fOut.flush()
        fOut.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Getting screenshot from current shown screen
fun getScreenShot(screenView: View): Bitmap {
    //val screenView = view.rootView
    screenView.isDrawingCacheEnabled = true
    screenView.buildDrawingCache(true)
    val bitmap = Bitmap.createBitmap(screenView.drawingCache)
    screenView.isDrawingCacheEnabled = false
    return bitmap
}

//rotating bitmap, since 4:3 ratio is used by default in the example
fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply {
        //postRotate(degrees)
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
