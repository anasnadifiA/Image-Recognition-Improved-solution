package com.upfeat.test

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.upfeat.test.databinding.ActivityMainBinding


/**
 * Main entry point into our app. This app follows the single-activity pattern, and all
 * functionality is implemented in the form of fragments.
 */
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
private val REQUEST_PERMISSION_CODE = 1
class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(
            this,
            PERMISSIONS_REQUIRED ,
            REQUEST_PERMISSION_CODE
        )
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Workaround for Android Q memory leak issue in IRequestFinishCallback$Stub.
            // (https://issuetracker.google.com/issues/139738913)
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    // overriding onRequestPermissionsResult, and making sure to have all permissions granted
    // to ensure correct app workflow execution
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            if (grantResults.contains( PackageManager.PERMISSION_DENIED )) {
                val dialog = AlertDialog.Builder(
                    this
                )
                dialog.setCancelable(false)
                    .setTitle("Notice")
                    .setMessage("Please make sure to allow all the permissions.")
                    .setPositiveButton(
                        "Ok"
                    ) { dialoginterface, i ->
                        dialoginterface.dismiss()
                        ActivityCompat.requestPermissions(
                            this,
                            PERMISSIONS_REQUIRED,
                            REQUEST_PERMISSION_CODE
                        )
                    }
                    .show()
            }
        }
    }


}
