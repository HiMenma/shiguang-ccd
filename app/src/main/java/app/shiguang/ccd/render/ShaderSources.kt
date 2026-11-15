package app.shiguang.ccd.render

/**
 * GLSL ES 3.0 sources.  The numeric behaviour mirrors the Kotlin reference
 * implementation in :imaging (ColorMath / ToneCurve) so that tests can reason
 * about the same maths.
 */
internal object ShaderSources {

    const val VERTEX = """
        #version 300 es
        layout(location = 0) in vec2 aPos;
        layout(location = 1) in vec2 aUv;
        uniform mat4 uTexMatrix;
        uniform float uMirror;
        out vec2 vUv;
        void main() {
            gl_Position = vec4(aPos, 0.0, 1.0);
            vec2 uv = (uTexMatrix * vec4(aUv, 0.0, 1.0)).xy;
            if (uMirror > 0.5) uv.x = 1.0 - uv.x;
            vUv = uv;
        }
    """

    private const val SCENE_HEADER_OES = """
        #version 300 es
        #extension GL_OES_EGL_image_external_essl3 : require
        precision highp float;
        uniform samplerExternalOES uTex;
    """

    private const val SCENE_HEADER_2D = """
        #version 300 es
        precision highp float;
        uniform sampler2D uTex;
    """

    private const val SCENE_BODY = """
        uniform vec3 uWb;
        uniform mat3 uMat;
        uniform float uExposure;
        uniform float uShoulder;
        uniform float uGamma;
        uniform float uToe;
        uniform float uBlackLift;
        uniform float uWhite;
        uniform float uSat;
        uniform vec3 uShadowTint;
        uniform vec3 uHighlightTint;
        in vec2 vUv;
        out vec4 fragColor;

        float tone(float x) {
            x *= exp2(uExposure);
            float s = clamp(uShoulder, 0.0, 0.85);
            if (s > 0.001) {
                float knee = 1.0 - s;
                if (x > knee) {
                    x = knee + s * tanh((x - knee) / s);
                }
            }
            float g = clamp(uGamma, 0.4, 2.5);
            x = pow(max(x, 0.0), g);
            float t = clamp(uToe, 0.0, 0.9);
            x = pow(max(x, 0.0), 1.0 / (1.0 + t));
            float lift = clamp(uBlackLift, 0.0, 0.3);
            x = lift + x * (1.0 - lift);
            x *= clamp(uWhite, 0.5, 1.0);
            return clamp(x, 0.0, 1.0);
        }

        void main() {
            vec3 c = texture(uTex, vUv).rgb;
            vec3 o = uMat * (c * uWb);
            o = vec3(tone(o.r), tone(o.g), tone(o.b));
            float l = dot(o, vec3(0.2126, 0.7152, 0.0722));
            o = mix(vec3(l), o, clamp(uSat, 0.0, 2.0));
            o += uShadowTint * (1.0 - l) + uHighlightTint * l;
            fragColor = vec4(clamp(o, 0.0, 1.0), 1.0);
        }
    """

    val SCENE_OES: String get() = SCENE_HEADER_OES + SCENE_BODY
    val SCENE_2D: String get() = SCENE_HEADER_2D + SCENE_BODY

    const val BRIGHT = """
        #version 300 es
        precision highp float;
        uniform sampler2D uTex;
        uniform float uThreshold;
        in vec2 vUv;
        out vec4 fragColor;
        void main() {
            vec3 c = texture(uTex, vUv).rgb;
            float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
            float k = max(l - uThreshold, 0.0) / max(1.0 - uThreshold, 0.001);
            fragColor = vec4(c * k, 1.0);
        }
    """

    const val BLUR = """
        #version 300 es
        precision highp float;
        uniform sampler2D uTex;
        uniform vec2 uDir;
        in vec2 vUv;
        out vec4 fragColor;
        void main() {
            float w0 = 0.2270270;
            float w1 = 0.1945946;
            float w2 = 0.1216216;
            float w3 = 0.0540540;
            float w4 = 0.0162162;
            vec3 sum = texture(uTex, vUv).rgb * w0;
            sum += texture(uTex, vUv + uDir * 1.0).rgb * w1;
            sum += texture(uTex, vUv - uDir * 1.0).rgb * w1;
            sum += texture(uTex, vUv + uDir * 2.0).rgb * w2;
            sum += texture(uTex, vUv - uDir * 2.0).rgb * w2;
            sum += texture(uTex, vUv + uDir * 3.0).rgb * w3;
            sum += texture(uTex, vUv - uDir * 3.0).rgb * w3;
            sum += texture(uTex, vUv + uDir * 4.0).rgb * w4;
            sum += texture(uTex, vUv - uDir * 4.0).rgb * w4;
            fragColor = vec4(sum, 1.0);
        }
    """

    const val COMPOSITE = """
        #version 300 es
        precision highp float;
        uniform sampler2D uScene;
        uniform sampler2D uBloom;
        uniform vec2 uTexel;
        uniform float uBloomStrength;
        uniform float uChroma;
        uniform float uSharpen;
        uniform float uGrain;
        uniform float uGrainSize;
        uniform float uSeed;
        uniform float uVignette;
        in vec2 vUv;
        out vec4 fragColor;

        float hash(vec2 p) {
            return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
        }

        void main() {
            vec2 uv = vUv;
            vec2 dir = uv - 0.5;
            vec3 c;
            c.r = texture(uScene, uv + dir * uChroma * 0.004).r;
            c.g = texture(uScene, uv).g;
            c.b = texture(uScene, uv - dir * uChroma * 0.004).b;

            c += texture(uBloom, uv).rgb * uBloomStrength;

            vec3 n = texture(uScene, uv + vec2(uTexel.x, 0.0)).rgb
                   + texture(uScene, uv - vec2(uTexel.x, 0.0)).rgb
                   + texture(uScene, uv + vec2(0.0, uTexel.y)).rgb
                   + texture(uScene, uv - vec2(0.0, uTexel.y)).rgb;
            if (uSharpen > 0.001) {
                c += (c - n * 0.25) * uSharpen * 2.0;
            }

            float g = hash(gl_FragCoord.xy * uGrainSize + uSeed) - 0.5;
            c += g * uGrain * 0.18;

            float v = smoothstep(0.35, 0.95, length(dir) * 1.4142);
            c *= 1.0 - v * uVignette;

            c += (hash(gl_FragCoord.xy * 1.7 + 13.0) - 0.5) / 255.0;
            fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
    """
}
