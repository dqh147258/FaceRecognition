package com.yxf.facerecognitionsample

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.yxf.facerecognition.FaceRecognition
import com.yxf.facerecognition.processor.*
import com.yxf.facerecognitionsample.databinding.ActivityFaceRecognitionBinding
import java.util.concurrent.Executors


class FaceRecognitionActivity : AppCompatActivity() {

    companion object {

        private val TAG = "FR." + "FaceRecognitionActivity"

    }


    private val vb by lazy { ActivityFaceRecognitionBinding.inflate(LayoutInflater.from(this)) }

    private lateinit var faceRecognition: FaceRecognition

    private var compareRecentChecked: Boolean = true
    private var addRecentChecked: Boolean = true

    private var seekBarProgress: Int = 70


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(vb.root)
        compareRecentChecked = vb.compareRecent.isChecked
        addRecentChecked = vb.addRecent.isChecked
        startCamera()
        initView()
        updateDifference(true)
    }

    private fun updateDifference(updateSeekBar: Boolean = false) {
        if (updateSeekBar) {
            vb.differenceSeekbar.progress = seekBarProgress
        }
        vb.differenceValue.text = "%.2f".format(seekBarProgress / 100.0f)
        faceRecognition.updateFaceProcessor(createFaceProcessor())
    }

    private fun initView() {
        vb.compareRecent.setOnCheckedChangeListener { buttonView, isChecked ->
            compareRecentChecked = isChecked
            faceRecognition.updateFaceProcessor(createFaceProcessor())
        }
        vb.addRecent.setOnCheckedChangeListener { buttonView, isChecked ->
            addRecentChecked = isChecked
            faceRecognition.updateFaceProcessor(createFaceProcessor())
        }
        vb.differenceSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBarProgress = progress
                updateDifference()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })
    }

    private fun createFaceProcessor(): FaceProcessor {
        return FaceProcessor(false).apply {
            addPipelineHandler(FaceRectPipelineHandler(vb.preview) {
                vb.root.post {
                    val lp = vb.rect.layoutParams as ViewGroup.MarginLayoutParams
                    lp.leftMargin = it.left
                    lp.topMargin = it.top
                    lp.width = it.width()
                    lp.height = it.height()
                    vb.rect.layoutParams = lp
                }
            })
            addPipelineHandler(RangePipelineHandler("??????????????????"))
            addPipelineHandler(AngleZPipelineHandler("????????????", 20.0f))
            //addPipelineHandler(WinkPipelineHandler("?????????"))
            addPipelineHandler(LivePipelineHandler("????????????????????????", this@FaceRecognitionActivity))
            val critical = seekBarProgress / 100.0f
            addPipelineHandler(FaceComparePipelineHandler("????????????", compareRecentChecked, critical) { result, faceInfo, difference ->
                if (result) {
                    vb.root.post {
                        vb.status.text = "orientation: ${faceInfo.getTypeName()}\n difference: ${"%.4f".format(difference)}"
                    }
                }
            })
            addPipelineHandler(SaveToPngPipelineHandler(getExternalFilesDir(null)!!.path + "/preview.png", 50, true))
            if (addRecentChecked) {
                addPipelineHandler(AddRecentFaceInfoPipelineHandler())
            }
        }
    }

    private fun startCamera() {
        faceRecognition = FaceRecognition.Builder(vb.preview)
            .setProcessSuccessfullyListener {

            }
            .setProcessFailedListener {
                vb.root.post {
                    vb.status.text = it
                }
            }
            .setExceptionListener {
                Log.e(TAG, "exception occurred", it)
            }
            .setFaceProcessor(createFaceProcessor())
            .build()

        faceRecognition.start()
    }


}