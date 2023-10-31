package com.upfeat.test

import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.view.View
import java.io.File
import java.io.FileOutputStream

/**
 * This is a Util file, containing useful functions used in the app's functions (file creation/storage)
 */

// Storing bitmap file, while taking into consideration device's OS
fun store(bm: Bitmap, fileName: String?) {
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
fun getScreenShot(view: View): Bitmap {
    val screenView = view.rootView
    screenView.isDrawingCacheEnabled = true
    val bitmap = Bitmap.createBitmap(screenView.drawingCache)
    screenView.isDrawingCacheEnabled = false
    return bitmap
}