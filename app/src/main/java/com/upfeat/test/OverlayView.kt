package com.upfeat.test

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.*
import kotlin.math.max

/**
 * The following class is a custom view class, showing a rectangular shape
 * following the boundaries of the detected object in the frame.
 *
 * We also show the detected object's label and the degree of confidence.
 */
class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    // A list of of Detection object, it will store the objects detected onResult() from the model
    private var results: List<Detection> = LinkedList<Detection>()
    // Objects controlling the drawn boxes boundaries and color
    private var boxPaint = Paint()
    private var bounds = Rect()

    // The objects controlling the label+threshold text's color & background color
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var scaleFactor: Float = 1f

    //The following mutable map, serves as a store for unique paints, for each object category
    private val customPaints = mutableMapOf<String, Int>()
    //The following object, is used in the algorithm to generate random unique colors to be assigned for a certain category
    var rnd = Random()

    init {
        initPaints()
    }

    // a function to clear the view's components
    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }
    // initially view's components and attributes for the bounding box and label+treshold text
    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        for (result in results) {
            val boundingBox = result.boundingBox

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect,
                // Either assign a unique color for a unique category, or use the pre-assigned one if already exist in map
                boxPaint.apply {
                    if(customPaints.containsKey(result.categories.first().label)){
                        boxPaint.color = customPaints[result.categories.first().label]!!
                    } else {
                        val color =
                            Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
                        boxPaint.color = color
                        customPaints[result.categories.first().label] = color
                    }
                }
            )

            // Create text to display alongside detected objects
            val drawableText =
                result.categories[0].label + " " +
                        String.format("%.2f", result.categories[0].score)

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + Companion.BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + Companion.BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw text for detected object
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }
    }

    fun setResults(
      detectionResults: MutableList<Detection>,
      imageHeight: Int,
      imageWidth: Int,
    ) {
        results = detectionResults

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
