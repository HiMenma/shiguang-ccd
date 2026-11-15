package app.shiguang.ccd.camera

import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import app.shiguang.ccd.render.LookRenderer
import app.shiguang.imaging.LookParams
import java.util.concurrent.Executor

/**
 * CameraX GPU effect: every preview frame is pushed through [LookRenderer]
 * before it reaches the PreviewView surface.
 *
 * All public entry points hop onto a private GL thread, so the processor is
 * safe regardless of which thread CameraX calls in on.
 */
class CcdSurfaceProcessor(
    private val paramsProvider: () -> LookParams,
    private val errorListener: (Throwable) -> Unit,
) : SurfaceProcessor, SurfaceTexture.OnFrameAvailableListener {

    private val thread = HandlerThread("ccd-gl")
    private val handler: Handler
    private val executor: Executor

    private var renderer: LookRenderer? = null
    private var released = false

    private var oesTexture = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var inputSurface: Surface? = null

    private val outputs = LinkedHashMap<SurfaceOutput, OutputTarget>()
    private val textureMatrix = FloatArray(16)
    private val surfaceMatrix = FloatArray(16)

    @Volatile
    var mirror: Boolean = false

    init {
        Matrix.setIdentityM(textureMatrix, 0)
        Matrix.setIdentityM(surfaceMatrix, 0)
        thread.start()
        handler = Handler(thread.looper)
        executor = Executor { command -> handler.post(command) }
        executor.execute {
            try {
                renderer = LookRenderer().apply { init() }
            } catch (t: Throwable) {
                errorListener(t)
            }
        }
    }

    override fun onInputSurface(request: SurfaceRequest) {
        executor.execute { handleInputSurface(request) }
    }

    override fun onOutputSurface(output: SurfaceOutput) {
        executor.execute { handleOutputSurface(output) }
    }

    override fun onFrameAvailable(st: SurfaceTexture) {
        if (released) return
        try {
            st.updateTexImage()
            st.getTransformMatrix(textureMatrix)
            drawFrame()
        } catch (t: Throwable) {
            errorListener(t)
        }
    }

    fun release() {
        executor.execute {
            if (released) return@execute
            released = true
            releaseInput()
            outputs.values.forEach { target ->
                runCatching { renderer?.detachWindow(target.window) }
                runCatching { target.output.close() }
            }
            outputs.clear()
            runCatching { renderer?.release() }
            renderer = null
            thread.quitSafely()
        }
    }

    private fun handleInputSurface(request: SurfaceRequest) {
        if (released) {
            request.willNotProvideSurface()
            return
        }
        val r = renderer
        if (r == null) {
            request.willNotProvideSurface()
            return
        }
        releaseInput()
        val width = request.resolution.width
        val height = request.resolution.height
        oesTexture = r.createOesTexture()
        val st = SurfaceTexture(oesTexture)
        st.setDefaultBufferSize(width, height)
        val surface = Surface(st)
        surfaceTexture = st
        inputSurface = surface
        request.provideSurface(surface, executor) {
            executor.execute {
                st.setOnFrameAvailableListener(null)
                runCatching { st.release() }
                surface.release()
            }
        }
        st.setOnFrameAvailableListener(this, handler)
    }

    private fun handleOutputSurface(output: SurfaceOutput) {
        if (released) {
            runCatching { output.close() }
            return
        }
        val r = renderer ?: return
        try {
            val surface = output.getSurface(executor) { runCatching { output.close() } }
            val window = r.attachWindow(surface)
            val size = output.size
            outputs[output] = OutputTarget(output, window, size.width, size.height)
        } catch (t: Throwable) {
            errorListener(t)
        }
    }

    private fun drawFrame() {
        if (released) return
        val r = renderer ?: return
        if (surfaceTexture == null || outputs.isEmpty()) return
        val params = paramsProvider()
        for ((output, target) in outputs) {
            try {
                output.updateTransformMatrix(surfaceMatrix, textureMatrix)
                r.renderPreview(
                    textureId = oesTexture,
                    textureMatrix = surfaceMatrix,
                    width = target.width,
                    height = target.height,
                    params = params,
                    mirror = mirror,
                    window = target.window,
                )
            } catch (t: Throwable) {
                errorListener(t)
            }
        }
    }

    private fun releaseInput() {
        surfaceTexture?.setOnFrameAvailableListener(null)
        runCatching { surfaceTexture?.release() }
        surfaceTexture = null
        runCatching { inputSurface?.release() }
        inputSurface = null
        if (oesTexture != 0 && renderer != null) {
            // Texture is owned by the released SurfaceTexture; nothing else to do.
            oesTexture = 0
        }
    }

    private class OutputTarget(
        val output: SurfaceOutput,
        val window: android.opengl.EGLSurface,
        val width: Int,
        val height: Int,
    )
}
