package app.shiguang.imaging

import kotlin.math.pow
import kotlin.math.tanh

/**
 * Parametric tone curve.  Deliberately simple and monotonic:
 *
 *   exposure -> shoulder -> mid gamma -> toe -> black lift -> white point
 *
 * The GLSL shader mirrors this exactly so preview and capture agree.
 */
object ToneCurve {

    fun apply(xIn: Float, p: LookParams): Float {
        var x = xIn * 2f.pow(p.exposure)

        val shoulder = p.shoulder.coerceIn(0f, 0.85f)
        if (shoulder > 1e-3f) {
            val knee = 1f - shoulder
            if (x > knee) {
                x = knee + shoulder * tanh((x - knee) / shoulder)
            }
        }

        val gamma = p.midGamma.coerceIn(0.4f, 2.5f)
        if (gamma != 1f && x > 0f) {
            x = x.pow(gamma)
        }

        val toe = p.toe.coerceIn(0f, 0.9f)
        if (toe > 1e-3f && x > 0f) {
            x = x.pow(1f / (1f + toe))
        }

        val lift = p.blackLift.coerceIn(0f, 0.3f)
        if (lift > 0f) {
            x = lift + x * (1f - lift)
        }

        x *= p.white.coerceIn(0.5f, 1f)
        return x.coerceIn(0f, 1f)
    }
}
