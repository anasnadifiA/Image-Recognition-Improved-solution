package com.upfeat.test

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.camera.core.*
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.upfeat.test.*
import com.upfeat.test.databinding.FragmentCameraBinding
import kotlinx.coroutines.*
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    private val TAG = "ObjectDetection"

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    // UI components, for implementing the functionalities from the test.
    private lateinit var seekBar: SeekBar
    private lateinit var tvValue: TextView
    private lateinit var btnSwitchCam : ImageView
    private lateinit var btnSnapShot : ImageView
    // This variable holds the last used camera orientation, upon click on btnSwitchCam, the opposite orientation is used.
    var currentCamDirection = CameraSelector.LENS_FACING_BACK





    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        seekBar = _fragmentCameraBinding!!.seekBar
        tvValue = _fragmentCameraBinding!!.tvValue
        btnSwitchCam =_fragmentCameraBinding!!.btnSwitchCam
        btnSnapShot = _fragmentCameraBinding!!.btnTakePhoto

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        seekBar.apply {
            tvValue.text = String.format("%d", 50 as Integer) + "%"
            progress =  50
            incrementProgressBy(1)
            max = 100
            setOnSeekBarChangeListener(object  : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    //Updating seekBar the textview's value onProgressChanged from the seekBar
                    tvValue.text = String.format("%d", p1 as Integer) + "%"
                    //Posting mutableLiveData threshold onProgressChanged from the seekBar
                    objectDetectorHelper.threshold.apply {
                        val newValue = (p1.toFloat()/100)
                        postValue(newValue)
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {

                }

                override fun onStopTrackingTouch(p0: SeekBar?) {

                }

            })
        }

        btnSwitchCam.setOnClickListener {
            //currentCamDirection's value is switched to opposite on click
            currentCamDirection = if(currentCamDirection == CameraSelector.LENS_FACING_BACK){
                CameraSelector.LENS_FACING_FRONT
            }else{
                CameraSelector.LENS_FACING_BACK
            }
            fragmentCameraBinding.viewFinder.post {
                // Set up the camera and its use cases
                setUpCamera()
            }
        }

        // Fade animation is used to show the user that a snapshot was taken.
        val fade = AlphaAnimation(1f, 0f)
        fade.duration = 500;
        fade.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
            }

            override fun onAnimationEnd(anim : Animation) {
                _fragmentCameraBinding!!.pnlFlash.visibility = View.GONE;
            }

            override fun onAnimationRepeat(p0: Animation?) {
            }
        });

        btnSnapShot.setOnClickListener {
            // on click a snap shot is taken, and saved at /Pictures/UpfeatTest/...
            with(_fragmentCameraBinding!!){
                pnlFlash.visibility = View.VISIBLE
                overlay.background = BitmapDrawable(
                    resources,
                    bitmapBuffer.rotate(
                        //depending on camera rotation, rotate the retrieved bitmap
                        if(currentCamDirection == LENS_FACING_BACK){
                            90f
                        }else{
                            -90f
                        }
                    )
                )
                store(getScreenShot(overlay), "snapShot_"+System.currentTimeMillis().toString().replace(":", ".").substring(6) + ".jpg")
                pnlFlash.startAnimation(fade)
                overlay.background = null
            }
        }

        isStoragePermissionGranted()

    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(currentCamDirection).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        isStoragePermissionGranted()
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            bitmapBuffer = Bitmap.createBitmap(
                              image.width,
                              image.height,
                              Bitmap.Config.ARGB_8888
                            )
                        }

                        //workaround for occasional FileNotFoundException exception
                        try {
                            detectObjects(image)
                        }catch (ignored : Exception){

                        }
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onResults(
      results: MutableList<Detection>?,
      inferenceTime: Long,
      imageHeight: Int,
      imageWidth: Int
    ) {
        activity?.runOnUiThread {
            // Pass necessary information to OverlayView for drawing on the canvas
            fragmentCameraBinding.overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )

            // Force a redraw
            fragmentCameraBinding.overlay.invalidate()

            //On objects detected, the count is displayed
            fragmentCameraBinding.tvCount.text = " Object(s) Found: ${results!!.size} "
        }
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isStoragePermissionGranted(): Boolean {
        return true
        /*return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                === PackageManager.PERMISSION_GRANTED
            ) {
                Log.v(TAG, "Permission is granted")
                true
            } else {
                Log.v(TAG, "Permission is revoked")
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                notifyUserAboutPermission()
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted")
            true
        }*/
    }
}
