package app.shiguang.ccd.camera

import android.content.Context
import android.util.Log
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.Executor

enum class FlashSetting {
    OFF,
    ON,
    AUTO,
}

internal fun FlashSetting.toImageCapture(): Int = when (this) {
    FlashSetting.OFF -> ImageCapture.FLASH_MODE_OFF
    FlashSetting.ON -> ImageCapture.FLASH_MODE_ON
    FlashSetting.AUTO -> ImageCapture.FLASH_MODE_AUTO
}

/**
 * Thin wrapper around CameraX: binds Preview (with our GPU effect) and
 * ImageCapture, and exposes the few controls the UI needs.
 */
class CameraSession(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val processor: CcdSurfaceProcessor,
    private val onError: (Throwable) -> Unit,
) {

    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null

    var lensFacing: Int = CameraSelector.LENS_FACING_BACK
        private set
    var flashMode: FlashSetting = FlashSetting.OFF
        private set

    private var started = false

    fun start() {
        if (started) return
        started = true
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                provider = future.get()
                bind()
            } catch (t: Throwable) {
                onError(t)
            }
        }, mainExecutor)
    }

    fun stop() {
        started = false
        runCatching { provider?.unbindAll() }
        camera = null
        imageCapture = null
    }

    fun switchLens() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bind()
    }

    fun cycleFlash() {
        flashMode = when (flashMode) {
            FlashSetting.OFF -> FlashSetting.ON
            FlashSetting.ON -> FlashSetting.AUTO
            FlashSetting.AUTO -> FlashSetting.OFF
        }
        imageCapture?.flashMode = flashMode.toImageCapture()
    }

    fun setZoom(ratio: Float) {
        runCatching { camera?.cameraControl?.setZoomRatio(ratio.coerceIn(1f, 6f)) }
    }

    fun setTorch(on: Boolean) {
        runCatching { camera?.cameraControl?.enableTorch(on) }
    }

    fun captureTo(file: File, onSaved: (File) -> Unit, onFailure: (Throwable) -> Unit) {
        val capture = imageCapture
        if (capture == null) {
            onFailure(IllegalStateException("相机尚未就绪"))
            return
        }
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(options, mainExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSaved(file)
            }

            override fun onError(exception: ImageCaptureException) {
                onFailure(exception)
            }
        })
    }

    private fun bind() {
        val boundProvider = provider ?: return
        boundProvider.unbindAll()

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val effect = PreviewEffect(
            CameraEffect.PREVIEW,
            mainExecutor,
            processor,
            Consumer { throwable ->
                Log.e(TAG, "camera effect error", throwable)
                onError(throwable)
            },
        )
        val preview = Preview.Builder().build()
        preview.surfaceProvider = previewView.surfaceProvider

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotationOfDisplay())
            .setFlashMode(flashMode.toImageCapture())
            .build()
        imageCapture = capture

        val group = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(capture)
            .addEffect(effect)
            .build()
        try {
            camera = boundProvider.bindToLifecycle(lifecycleOwner, selector, group)
        } catch (t: Throwable) {
            Log.e(TAG, "bind failed", t)
            onError(t)
        }
    }

    private fun rotationOfDisplay(): Int = previewView.display?.rotation ?: Surface.ROTATION_0

    private companion object {
        const val TAG = "CameraSession"
    }
}


private class PreviewEffect(
    targets: Int,
    executor: Executor,
    processor: SurfaceProcessor,
    errorListener: Consumer<Throwable>,
) : CameraEffect(targets, executor, processor, errorListener)
