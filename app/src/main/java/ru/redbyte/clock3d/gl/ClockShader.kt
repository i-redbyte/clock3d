package ru.redbyte.clock3d.gl

import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Mat4
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.abs
import ru.redbyte.redbytefx.and
import ru.redbyte.redbytefx.choose
import ru.redbyte.redbytefx.dot
import ru.redbyte.redbytefx.float2
import ru.redbyte.redbytefx.float3
import ru.redbyte.redbytefx.fract
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.length
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.lt
import ru.redbyte.redbytefx.max
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.normalize
import ru.redbyte.redbytefx.not
import ru.redbyte.redbytefx.or
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.pow
import ru.redbyte.redbytefx.saturate
import ru.redbyte.redbytefx.scene.instanceModel
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.smoothstep
import ru.redbyte.redbytefx.stdlib.cosinePalette
import ru.redbyte.redbytefx.stdlib.easeInOutSine
import ru.redbyte.redbytefx.stdlib.grain
import ru.redbyte.redbytefx.stdlib.lambert
import ru.redbyte.redbytefx.stdlib.pulse
import ru.redbyte.redbytefx.stdlib.vignette
import ru.redbyte.redbytefx.stdlib.wrapLambert
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal class ClockShaderHandles(
    val program: ShaderProgram,
    val view: Uniform<Mat4>,
    val projection: Uniform<Mat4>,
    val light: Uniform<Vec3<Flt<High>>>,
    val mode: Uniform<Flt<High>>,
    val wave: Uniform<Flt<High>>,
    val handReach: Uniform<Vec3<Flt<High>>>,
    val material: Uniform<Flt<High>>,
)

internal fun buildClockShader(): ClockShaderHandles {
    lateinit var view: Uniform<Mat4>
    lateinit var projection: Uniform<Mat4>
    lateinit var light: Uniform<Vec3<Flt<High>>>
    lateinit var mode: Uniform<Flt<High>>
    lateinit var wave: Uniform<Flt<High>>
    lateinit var handReach: Uniform<Vec3<Flt<High>>>
    lateinit var material: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        view = uniformMat4("view")
        projection = uniformMat4("projection")
        light = uniformVec3("light", 0.35f, 0.8f, 0.55f)
        mode = uniform("mode", 0f)
        wave = uniform("wave", 0f)
        handReach = uniformVec3("handReach", 0.36f, 0.46f, 0.54f)
        material = uniform("material", -1f)
        val facing = varyingVec3("normal")
        val place = varyingVec3("place")
        val matId = varyingFloat("matId")
        val layer = varyingFloat("layer")
        vertex {
            val model = instanceModel()
            val position = attributeVec3("position")
            val normal = attributeVec3("normal")
            val texcoord = attributeVec2("uv")
            val world = model * vec4(position.x, position.y, position.z, 1f.lit)
            val worldN = model * vec4(normal.x, normal.y, normal.z, 0f.lit)
            facing.set(vec3(worldN.x, worldN.y, worldN.z))
            place.set(vec3(world.x, world.y, world.z))
            matId.set(ifElse(material.expr lt 0f, texcoord.x, material.expr))
            layer.set(texcoord.y)
            glPosition(projection.expr * (view.expr * world))
        }
        fragment {
            val lambertShade = wrapLambert(facing.expr, light.expr, 0.32f)
            val isClassic = mode.expr lt 0.5f.lit
            val isDali = (mode.expr gt 0.5f.lit) and (mode.expr lt 1.5f.lit)
            val isSpheres = (mode.expr gt 1.5f.lit) and (mode.expr lt 2.5f.lit)
            val isIce = mode.expr gt 2.5f.lit
            val wrapShade = abs(dot(normalize(facing.expr), normalize(light.expr)))
            val shade = choose(mode.expr) {
                on(3f) { max(lambert(facing.expr, light.expr), 0.22f.lit) * 0.55f.lit + 0.28f.lit }
                on(1f) { wrapShade * 0.38f.lit + 0.52f.lit }
                otherwise { lambertShade }
            }
            val hour = (matId.expr gt 0.5f.lit) and (matId.expr lt 1.5f.lit)
            val minute = (matId.expr gt 1.5f.lit) and (matId.expr lt 2.5f.lit)
            val second = (matId.expr gt 2.5f.lit) and (matId.expr lt 3.5f.lit)
            val rim = (matId.expr gt 4.5f.lit) and (matId.expr lt 5.5f.lit)
            val backdrop = (matId.expr gt 6.5f.lit) and (matId.expr lt 7.5f.lit)
            val mist = matId.expr gt 7.5f.lit
            val uv = float2(place.expr.x * 0.35f.lit + 0.5f.lit, place.expr.y * 0.35f.lit + 0.5f.lit)
            val breath = easeInOutSine(pulse(wave.expr, 0.35f, 0.2f))
            val field = grain(uv, fract(wave.expr * 0.07f.lit), 18f) * 0.5f.lit + 0.5f.lit
            val tone = field * 0.65f.lit + breath * 0.35f.lit
            val skyBias = ifElse(
                isClassic,
                float3(0.10f, 0.20f, 0.15f),
                ifElse(
                    isDali,
                    float3(0.16f, 0.11f, 0.08f),
                    ifElse(isSpheres, float3(0.32f, 0.08f, 0.06f), float3(0.12f, 0.22f, 0.32f)),
                ),
            )
            val skyAmp = ifElse(
                isClassic,
                float3(0.04f, 0.08f, 0.05f),
                ifElse(
                    isDali,
                    float3(0.05f, 0.04f, 0.03f),
                    ifElse(isSpheres, float3(0.10f, 0.04f, 0.02f), float3(0.04f, 0.06f, 0.07f)),
                ),
            )
            val skyFreq = ifElse(
                isClassic,
                float3(1f, 1.2f, 0.8f),
                ifElse(
                    isDali,
                    float3(1f, 0.8f, 0.6f),
                    ifElse(isSpheres, float3(1.1f, 0.7f, 0.4f), float3(0.8f, 1.1f, 1.3f)),
                ),
            )
            val skyPhase = vec3(
                ifElse(isClassic, 0.1f.lit, ifElse(isDali, 0.05f.lit, ifElse(isSpheres, 0f.lit, 0.15f.lit))),
                ifElse(isClassic, 0.25f.lit, ifElse(isDali, 0.2f.lit, ifElse(isSpheres, 0.15f.lit, 0.28f.lit))),
                ifElse(isClassic, 0.4f.lit, ifElse(isDali, 0.35f.lit, ifElse(isSpheres, 0.35f.lit, 0.45f.lit))),
            )
            val rawSky = cosinePalette(tone, skyBias, skyAmp, skyFreq, skyPhase)
            val skyFloor = ifElse(
                isClassic,
                float3(0.05f, 0.12f, 0.09f),
                ifElse(
                    isDali,
                    float3(0.10f, 0.07f, 0.05f),
                    ifElse(isSpheres, float3(0.18f, 0.04f, 0.03f), float3(0.08f, 0.16f, 0.26f)),
                ),
            )
            val sky = vec3(
                max(rawSky.x, skyFloor.x),
                max(rawSky.y, skyFloor.y),
                max(rawSky.z, skyFloor.z),
            )
            val dust = grain(uv, fract(wave.expr * 0.31f.lit), 90f) * ifElse(isIce, 0.02f.lit, 0.05f.lit)
            val fade = vignette(uv, 0.15f.lit, 1.15f.lit)
            val back = vec3(sky.x + dust, sky.y + dust, sky.z + dust) *
                (ifElse(isIce, 0.80f.lit, 0.62f.lit) + fade * 0.45f.lit)
            val xy = float2(place.expr.x, place.expr.y)
            val radial = length(xy)
            val drift = radial - field * 0.28f.lit
            val ringA = 1f.lit - abs(fract(drift * 0.72f.lit - wave.expr * 0.052f.lit) * 2f.lit - 1f.lit)
            val ringB = 1f.lit - abs(fract(drift * 0.40f.lit - wave.expr * 0.031f.lit + 0.45f.lit) * 2f.lit - 1f.lit)
            val swell = pow(ringA, 2.8f.lit) * 0.70f.lit + pow(ringB, 2.2f.lit) * 0.45f.lit
            val near = saturate(1.05f.lit - radial * 0.85f.lit)
            val breathFog = field * 0.80f.lit + dust * 3f.lit
            val fog = saturate(near * 0.12f.lit + swell * (0.28f.lit + breathFog * 0.55f.lit))
            val mistRgb = vec3(0.66f.lit, 0.84f.lit, 0.97f.lit)
            val icyBack = vec3(
                back.x + mistRgb.x * fog * 0.55f.lit,
                back.y + mistRgb.y * fog * 0.48f.lit,
                back.z + mistRgb.z * fog * 0.40f.lit,
            )
            val veil = smoothstep(0.32f.lit, 0.52f.lit, radial) *
                (1f.lit - smoothstep(0.95f.lit, 1.85f.lit, radial))
            val sheet = 0.50f.lit + layer.expr * 0.40f.lit
            val mistAlpha = saturate(veil * (0.04f.lit + swell * 0.28f.lit + breathFog * 0.10f.lit) * sheet)
            val classicFace = vec3(0.10f.lit, 0.36f.lit, 0.28f.lit)
            val classicHour = vec3(0.90f.lit, 0.76f.lit, 0.32f.lit)
            val classicMinute = vec3(0.90f.lit, 0.93f.lit, 0.88f.lit)
            val classicSecond = vec3(0.92f.lit, 0.28f.lit, 0.22f.lit)
            val classicDigit = vec3(0.86f.lit, 0.74f.lit, 0.30f.lit)
            val classicRim = vec3(0.16f.lit, 0.48f.lit, 0.36f.lit)
            val classicAccent = vec3(0.78f.lit, 0.68f.lit, 0.28f.lit)
            val daliFace = vec3(
                0.20f.lit + field * 0.08f.lit,
                0.18f.lit + field * 0.06f.lit,
                0.16f.lit + field * 0.05f.lit,
            )
            val daliHour = vec3(0.90f.lit, 0.72f.lit, 0.22f.lit)
            val daliMinute = vec3(0.86f.lit, 0.68f.lit, 0.20f.lit)
            val daliSecond = vec3(0.94f.lit, 0.42f.lit, 0.14f.lit)
            val daliDigit = vec3(0.88f.lit, 0.70f.lit, 0.20f.lit)
            val daliRim = vec3(0.78f.lit, 0.80f.lit, 0.84f.lit)
            val daliAccent = vec3(0.84f.lit, 0.70f.lit, 0.26f.lit)
            val hand = hour or minute or second
            val iceGleam = pulse(wave.expr, 1.6f, 0.3f)
            val strikeOffset = ifElse(
                hour,
                LightningStrike.HOUR_OFFSET.lit,
                ifElse(minute, LightningStrike.MINUTE_OFFSET.lit, LightningStrike.SECOND_OFFSET.lit),
            )
            val strikeTempo = ifElse(
                second,
                LightningStrike.SECOND_TEMPO.lit,
                ifElse(minute, LightningStrike.MINUTE_TEMPO.lit, LightningStrike.HOUR_TEMPO.lit),
            )
            val strikePhase = fract(wave.expr * strikeTempo + strikeOffset)
            val growEnd = 0.16f.lit
            val holdEnd = 0.28f.lit
            val dieEnd = 0.46f.lit
            val envelope = ifElse(
                strikePhase lt growEnd,
                easeInOutSine(saturate(strikePhase * 6.25f)),
                ifElse(
                    strikePhase lt holdEnd,
                    1f.lit,
                    ifElse(
                        strikePhase lt dieEnd,
                        easeInOutSine(saturate((dieEnd - strikePhase) * 5.56f)),
                        0f.lit,
                    ),
                ),
            )
            val maxReach = ifElse(hour, handReach.expr.x, ifElse(minute, handReach.expr.y, handReach.expr.z))
            val reach = envelope * maxReach
            discard(
                hand and isSpheres and (
                    (envelope lt 0.02f.lit) or
                        (radial gt reach) or
                        (radial gt SpiralBound.OUTER.lit)
                    ),
            )
            val spark = pulse(wave.expr, 2.8f, 0f)
            val flicker = pulse(wave.expr, 5.2f, 1.1f)
            val surge = envelope * (0.55f.lit + spark * 0.30f.lit + flicker * 0.25f.lit)
            val commBead = vec3(
                0.55f.lit + field * 0.28f.lit,
                0.10f.lit + field * 0.12f.lit,
                0.07f.lit + pulse(wave.expr, 1.1f, 0.4f) * 0.08f.lit,
            )
            val halo = layer.expr gt 0.5f.lit
            val commHour = ifElse(
                halo,
                vec3(0.55f.lit, 0.85f.lit, 1f.lit) * (surge * 0.85f.lit),
                vec3(1f.lit, 0.98f.lit, 0.85f.lit) * (surge * 1.75f.lit),
            )
            val commMinute = ifElse(
                halo,
                vec3(0.35f.lit, 0.95f.lit, 1f.lit) * (surge * 0.80f.lit),
                vec3(0.85f.lit, 1f.lit, 1f.lit) * (surge * 1.75f.lit),
            )
            val commSecond = ifElse(
                halo,
                vec3(1f.lit, 0.45f.lit, 0.2f.lit) * (surge * 0.95f.lit),
                vec3(1f.lit, 0.92f.lit, 0.75f.lit) * (surge * 2.0f.lit),
            )
            val commDigit = vec3(1f.lit, 0.88f.lit, 0.35f.lit)
            val commRim = vec3(0.85f.lit, 0.15f.lit, 0.12f.lit)
            val commAccent = vec3(1f.lit, 0.75f.lit, 0.15f.lit)
            val nrm = normalize(facing.expr)
            val viewDir = normalize(vec3(0f.lit, 0.04f.lit, 4f.lit) - place.expr)
            val ndotv = abs(dot(nrm, viewDir))
            val iceFresnel = pow(1f.lit - saturate(ndotv), 1.55f.lit)
            val facet = layer.expr
            val iceFace = vec3(
                0.78f.lit + iceFresnel * 0.16f.lit + facet * 0.04f.lit,
                0.90f.lit + iceFresnel * 0.08f.lit,
                0.98f.lit + iceFresnel * 0.04f.lit,
            )
            val iceHour = vec3(0.46f.lit, 0.64f.lit, 0.76f.lit)
            val iceMinute = vec3(0.52f.lit, 0.70f.lit, 0.82f.lit)
            val iceSecond = vec3(0.28f.lit, 0.66f.lit, 0.80f.lit) * (0.75f.lit + iceGleam * 0.18f.lit)
            val iceDigit = vec3(0.48f.lit, 0.68f.lit, 0.82f.lit)
            val iceRim = vec3(0.34f.lit, 0.52f.lit, 0.66f.lit)
            val iceAccent = vec3(0.28f.lit, 0.42f.lit, 0.54f.lit)
            val face = ifElse(isClassic, classicFace, ifElse(isDali, daliFace, ifElse(isSpheres, commBead, iceFace)))
            val hourC = ifElse(isClassic, classicHour, ifElse(isDali, daliHour, ifElse(isSpheres, commHour, iceHour)))
            val minuteC =
                ifElse(isClassic, classicMinute, ifElse(isDali, daliMinute, ifElse(isSpheres, commMinute, iceMinute)))
            val secondC =
                ifElse(isClassic, classicSecond, ifElse(isDali, daliSecond, ifElse(isSpheres, commSecond, iceSecond)))
            val digitC =
                ifElse(isClassic, classicDigit, ifElse(isDali, daliDigit, ifElse(isSpheres, commDigit, iceDigit)))
            val rimC = ifElse(isClassic, classicRim, ifElse(isDali, daliRim, ifElse(isSpheres, commRim, iceRim)))
            val accentC =
                ifElse(isClassic, classicAccent, ifElse(isDali, daliAccent, ifElse(isSpheres, commAccent, iceAccent)))
            val solid = choose(matId.expr) {
                on(1f) { hourC }
                on(2f) { minuteC }
                on(3f) { secondC }
                on(4f) { digitC }
                on(5f) { rimC }
                on(6f) { accentC }
                otherwise { face }
            }
            val glow = ifElse(
                hand and isSpheres,
                ifElse(halo, surge * 0.70f.lit, surge * 2.10f.lit),
                0f.lit,
            )
            val glass = isIce and (matId.expr lt 0.5f.lit)
            val ambient = ifElse(
                glass,
                0.95f.lit + iceFresnel * 0.35f.lit,
                ifElse(isClassic, 0.62f.lit, ifElse(isDali, 0.72f.lit, ifElse(isSpheres, 0.50f.lit, 0.52f.lit))),
            )
            val spec = pow(shade, ifElse(isIce, 28f.lit, 14f.lit)) * ifElse(
                rim,
                ifElse(isDali, 0.18f.lit, ifElse(isIce, 0.28f.lit, 0.5f.lit)),
                ifElse(isIce, 0.10f.lit + iceFresnel * 0.22f.lit + iceGleam * 0.06f.lit, 0.14f.lit),
            )
            val iceSolid = isIce and not(glass) and not(backdrop) and not(mist)
            val iceEmit = ifElse(iceSolid, 0.04f.lit, 0f.lit)
            val litRgb = solid * (ambient + shade * 0.55f.lit + iceEmit) +
                vec3(spec, spec, spec) * ifElse(iceSolid, 0.20f.lit, 1f.lit) +
                solid * glow
            discard(mist and (mistAlpha lt 0.012f.lit))
            val rgb = ifElse(
                mist,
                mistRgb * (0.45f.lit + breathFog * 0.40f.lit + swell * 0.25f.lit),
                ifElse(backdrop, ifElse(isIce, icyBack, back), litRgb),
            )
            val alpha = ifElse(mist, mistAlpha, ifElse(glass, 0.28f.lit + iceFresnel * 0.50f.lit, 1f.lit))
            vec4(rgb.x, rgb.y, rgb.z, alpha)
        }
    }
    return ClockShaderHandles(program, view, projection, light, mode, wave, handReach, material)
}
