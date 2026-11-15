package app.shiguang.ccd.render

import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLUtils
import android.view.Surface
import app.shiguang.imaging.ColorMath
import app.shiguang.imaging.LookParams
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Owns one EGL context and renders the whole look chain:
 *
 *   scene (WB -> matrix -> tone -> split tone -> saturation)
 *     -> bright pass -> separable blur (x2)
 *     -> composite (bloom, chroma, sharpen, grain, vignette, dither)
 *
 * Not thread safe: create one instance per GL thread.
 */
class LookRenderer {

    private val egl = EglCore()

    private var sceneOesProgram = 0
    private var scene2dProgram = 0
    private var brightProgram = 0
    private var blurProgram = 0
    private var compositeProgram = 0

    private val uniformCache = HashMap<Int, HashMap<String, Int>>()
    private var quadVbo = 0

    private var sceneTex = 0
    private var sceneFbo = 0
    private var sceneW = 0
    private var sceneH = 0

    private var bloomTexA = 0
    private var bloomFboA = 0
    private var bloomTexB = 0
    private var bloomFboB = 0
    private var bloomW = 0
    private var bloomH = 0

    private var outTex = 0
    private var outFbo = 0
    private var outW = 0
    private var outH = 0

    private var frameSeed = 0f

    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f,
    )

    private val yFlip = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, -1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 1f, 0f, 1f,
    )

    fun init() {
        egl.init()
        sceneOesProgram = GlUtil.link(ShaderSources.VERTEX, ShaderSources.SCENE_OES)
        scene2dProgram = GlUtil.link(ShaderSources.VERTEX, ShaderSources.SCENE_2D)
        brightProgram = GlUtil.link(ShaderSources.VERTEX, ShaderSources.BRIGHT)
        blurProgram = GlUtil.link(ShaderSources.VERTEX, ShaderSources.BLUR)
        compositeProgram = GlUtil.link(ShaderSources.VERTEX, ShaderSources.COMPOSITE)
        setupQuad()
        GlUtil.checkError("init")
    }

    fun release() {
        listOf(sceneOesProgram, scene2dProgram, brightProgram, blurProgram, compositeProgram)
            .filter { it != 0 }
            .forEach { GLES30.glDeleteProgram(it) }
        if (quadVbo != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(quadVbo), 0)
        }
        deleteTarget(sceneTex, sceneFbo)
        deleteTarget(bloomTexA, bloomFboA)
        deleteTarget(bloomTexB, bloomFboB)
        deleteTarget(outTex, outFbo)
        egl.release()
    }

    fun createOesTexture(): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ids[0])
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR,
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR,
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE,
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE,
        )
        return ids[0]
    }

    fun attachWindow(surface: Surface): EGLSurface = egl.createWindowSurface(surface)

    fun detachWindow(window: EGLSurface) = egl.releaseSurface(window)

    fun renderPreview(
        textureId: Int,
        textureMatrix: FloatArray,
        width: Int,
        height: Int,
        params: LookParams,
        mirror: Boolean,
        window: EGLSurface,
    ) {
        if (width <= 0 || height <= 0) return
        egl.makeCurrent(window)
        frameSeed = ((frameSeed + 17.13f) % 4096f)
        drawFrame(
            inputTexture = textureId,
            external = true,
            textureMatrix = textureMatrix,
            outWidth = width,
            outHeight = height,
            params = params,
            mirror = mirror,
            targetFramebuffer = 0,
        )
        egl.swap(window)
    }

    fun renderBitmap(source: Bitmap, params: LookParams, mirror: Boolean = false): Bitmap {
        val width = source.width
        val height = source.height
        egl.makePbufferCurrent()

        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        val inputTexture = ids[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTexture)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, source, 0)

        ensureOutput(width, height)
        drawFrame(
            inputTexture = inputTexture,
            external = false,
            textureMatrix = yFlip,
            outWidth = width,
            outHeight = height,
            params = params,
            mirror = mirror,
            targetFramebuffer = outFbo,
        )

        val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, outFbo)
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        buffer.rewind()

        val raw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        raw.copyPixelsFromBuffer(buffer)
        GLES30.glDeleteTextures(1, intArrayOf(inputTexture), 0)

        // glReadPixels is bottom-up; flip once.
        return Bitmap.createBitmap(
            raw, 0, 0, width, height,
            Matrix().apply { postScale(1f, -1f) },
            true,
        )
    }

    private fun drawFrame(
        inputTexture: Int,
        external: Boolean,
        textureMatrix: FloatArray,
        outWidth: Int,
        outHeight: Int,
        params: LookParams,
        mirror: Boolean,
        targetFramebuffer: Int,
    ) {
        val safe = params.sanitized()
        val scale = safe.renderScale
        val sw = max(1, (outWidth * scale).roundToInt())
        val sh = max(1, (outHeight * scale).roundToInt())
        ensureScene(sw, sh)

        // 1. scene
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, sceneFbo)
        GLES30.glViewport(0, 0, sw, sh)
        val sceneProgram = if (external) sceneOesProgram else scene2dProgram
        GLES30.glUseProgram(sceneProgram)
        bindInput(sceneProgram, inputTexture, external)
        uploadSceneUniforms(sceneProgram, safe, mirror)
        GLES30.glUniformMatrix4fv(loc(sceneProgram, "uTexMatrix"), 1, false, textureMatrix, 0)
        drawQuad()

        // 2. bright pass at quarter resolution
        val bw = max(1, sw / 4)
        val bh = max(1, sh / 4)
        ensureBloom(bw, bh)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFboA)
        GLES30.glViewport(0, 0, bw, bh)
        GLES30.glUseProgram(brightProgram)
        bindTexture(brightProgram, sceneTex, 0, "uTex")
        GLES30.glUniform1f(loc(brightProgram, "uThreshold"), safe.bloomThreshold)
        GLES30.glUniformMatrix4fv(loc(brightProgram, "uTexMatrix"), 1, false, identity, 0)
        GLES30.glUniform1f(loc(brightProgram, "uMirror"), 0f)
        drawQuad()

        // 3. separable blur: A -> B (horizontal), B -> A (vertical)
        GLES30.glUseProgram(blurProgram)
        GLES30.glUniformMatrix4fv(loc(blurProgram, "uTexMatrix"), 1, false, identity, 0)
        GLES30.glUniform1f(loc(blurProgram, "uMirror"), 0f)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFboB)
        GLES30.glViewport(0, 0, bw, bh)
        bindTexture(blurProgram, bloomTexA, 0, "uTex")
        GLES30.glUniform2f(loc(blurProgram, "uDir"), 1f / bw, 0f)
        drawQuad()

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFboA)
        GLES30.glViewport(0, 0, bw, bh)
        bindTexture(blurProgram, bloomTexB, 0, "uTex")
        GLES30.glUniform2f(loc(blurProgram, "uDir"), 0f, 1f / bh)
        drawQuad()

        // 4. composite to the requested target
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, targetFramebuffer)
        GLES30.glViewport(0, 0, outWidth, outHeight)
        GLES30.glUseProgram(compositeProgram)
        bindTexture(compositeProgram, sceneTex, 0, "uScene")
        bindTexture(compositeProgram, bloomTexA, 1, "uBloom")
        GLES30.glUniform2f(loc(compositeProgram, "uTexel"), 1f / sw, 1f / sh)
        GLES30.glUniform1f(loc(compositeProgram, "uBloomStrength"), safe.bloomStrength)
        GLES30.glUniform1f(loc(compositeProgram, "uChroma"), safe.chromaticAberration)
        GLES30.glUniform1f(loc(compositeProgram, "uSharpen"), safe.sharpen)
        GLES30.glUniform1f(loc(compositeProgram, "uGrain"), safe.grainAmount)
        GLES30.glUniform1f(
            loc(compositeProgram, "uGrainSize"),
            safe.grainSize * max(0.5f, outHeight / 1080f),
        )
        GLES30.glUniform1f(loc(compositeProgram, "uSeed"), frameSeed)
        GLES30.glUniform1f(loc(compositeProgram, "uVignette"), safe.vignette)
        GLES30.glUniformMatrix4fv(loc(compositeProgram, "uTexMatrix"), 1, false, identity, 0)
        GLES30.glUniform1f(loc(compositeProgram, "uMirror"), 0f)
        drawQuad()

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GlUtil.checkError("drawFrame")
    }

    private fun uploadSceneUniforms(program: Int, p: LookParams, mirror: Boolean) {
        val wb = ColorMath.whiteBalanceGains(p.kelvin, p.tint)
        GLES30.glUniform3f(loc(program, "uWb"), wb[0], wb[1], wb[2])
        GLES30.glUniformMatrix3fv(loc(program, "uMat"), 1, false, columnMajor(p.matrix), 0)
        GLES30.glUniform1f(loc(program, "uExposure"), p.exposure)
        GLES30.glUniform1f(loc(program, "uShoulder"), p.shoulder)
        GLES30.glUniform1f(loc(program, "uGamma"), p.midGamma)
        GLES30.glUniform1f(loc(program, "uToe"), p.toe)
        GLES30.glUniform1f(loc(program, "uBlackLift"), p.blackLift)
        GLES30.glUniform1f(loc(program, "uWhite"), p.white)
        GLES30.glUniform1f(loc(program, "uSat"), p.saturation)
        GLES30.glUniform3f(loc(program, "uShadowTint"), p.shadowTint[0], p.shadowTint[1], p.shadowTint[2])
        GLES30.glUniform3f(
            loc(program, "uHighlightTint"),
            p.highlightTint[0], p.highlightTint[1], p.highlightTint[2],
        )
        GLES30.glUniform1f(loc(program, "uMirror"), if (mirror) 1f else 0f)
    }

    private fun bindInput(program: Int, textureId: Int, external: Boolean) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        if (external) {
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        } else {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        }
        GLES30.glUniform1i(loc(program, "uTex"), 0)
    }

    private fun bindTexture(program: Int, textureId: Int, unit: Int, uniformName: String) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(loc(program, uniformName), unit)
    }

    private fun ensureScene(w: Int, h: Int) {
        if (w == sceneW && h == sceneH && sceneTex != 0) return
        deleteTarget(sceneTex, sceneFbo)
        sceneTex = GlUtil.createTexture(w, h)
        sceneFbo = GlUtil.createFramebuffer(sceneTex)
        sceneW = w
        sceneH = h
    }

    private fun ensureBloom(w: Int, h: Int) {
        if (w == bloomW && h == bloomH && bloomTexA != 0) return
        deleteTarget(bloomTexA, bloomFboA)
        deleteTarget(bloomTexB, bloomFboB)
        bloomTexA = GlUtil.createTexture(w, h)
        bloomFboA = GlUtil.createFramebuffer(bloomTexA)
        bloomTexB = GlUtil.createTexture(w, h)
        bloomFboB = GlUtil.createFramebuffer(bloomTexB)
        bloomW = w
        bloomH = h
    }

    private fun ensureOutput(w: Int, h: Int) {
        if (w == outW && h == outH && outTex != 0) return
        deleteTarget(outTex, outFbo)
        outTex = GlUtil.createTexture(w, h)
        outFbo = GlUtil.createFramebuffer(outTex)
        outW = w
        outH = h
    }

    private fun deleteTarget(texture: Int, framebuffer: Int) {
        if (framebuffer != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        if (texture != 0) GLES30.glDeleteTextures(1, intArrayOf(texture), 0)
    }

    private fun setupQuad() {
        val data = floatArrayOf(
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f,
            -1f, 1f, 0f, 1f,
            1f, 1f, 1f, 1f,
        )
        val buffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(data).position(0)

        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        quadVbo = ids[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.size * 4, buffer, GLES30.GL_STATIC_DRAW)
    }

    private fun drawQuad() {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun loc(program: Int, name: String): Int =
        uniformCache.getOrPut(program) { HashMap() }
            .getOrPut(name) { GLES30.glGetUniformLocation(program, name) }

    private fun columnMajor(rowMajor: List<Float>): FloatArray = floatArrayOf(
        rowMajor[0], rowMajor[3], rowMajor[6],
        rowMajor[1], rowMajor[4], rowMajor[7],
        rowMajor[2], rowMajor[5], rowMajor[8],
    )
}
