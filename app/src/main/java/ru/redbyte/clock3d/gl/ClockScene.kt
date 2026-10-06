package ru.redbyte.clock3d.gl

import ru.redbyte.redbytefx.gl.compose.BlendFactor
import ru.redbyte.redbytefx.gl.compose.GlFrame
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.GlPipeline
import ru.redbyte.redbytefx.scene.MATRIX_FLOATS
import ru.redbyte.redbytefx.scene.lookAt
import ru.redbyte.redbytefx.scene.ortho
import java.util.Calendar

enum class ClockMode(val shaderValue: Float) {
    Classic(0f),
    Dali(1f),
    Spheres(2f),
    Ice(3f),
    ;

    companion object {
        fun fromLaunchExtra(value: String?): ClockMode =
            when (value?.lowercase()) {
                "dali" -> Dali
                "spheres" -> Spheres
                "ice" -> Ice
                else -> Classic
            }
    }
}

internal class ClockHandsSet(
    val hour: GlMesh,
    val minute: GlMesh,
    val second: GlMesh,
)

internal object LightningStrike {
    const val WAVE_RATE: Float = 1.6f
    const val HOUR_TEMPO: Float = 0.38f
    const val MINUTE_TEMPO: Float = 0.48f
    const val SECOND_TEMPO: Float = 0.72f
    const val HOUR_OFFSET: Float = 0f
    const val MINUTE_OFFSET: Float = 0.41f
    const val SECOND_OFFSET: Float = 0.73f

    fun generation(wave: Float, tempo: Float, offset: Float): Int =
        kotlin.math.floor((wave * tempo + offset).toDouble()).toInt()
}

internal class LightningBank {
    private var hourGen: Int = Int.MIN_VALUE
    private var minuteGen: Int = Int.MIN_VALUE
    private var secondGen: Int = Int.MIN_VALUE
    private var hour: GlMesh? = null
    private var minute: GlMesh? = null
    private var second: GlMesh? = null

    fun hands(wave: Float): ClockHandsSet {
        val nextHour = LightningStrike.generation(wave, LightningStrike.HOUR_TEMPO, LightningStrike.HOUR_OFFSET)
        val nextMinute = LightningStrike.generation(wave, LightningStrike.MINUTE_TEMPO, LightningStrike.MINUTE_OFFSET)
        val nextSecond = LightningStrike.generation(wave, LightningStrike.SECOND_TEMPO, LightningStrike.SECOND_OFFSET)
        var hourMesh = hour
        var minuteMesh = minute
        var secondMesh = second
        if (hourMesh == null || nextHour != hourGen) {
            hourGen = nextHour
            hourMesh = ClockHands.lightning(SpiralBound.capped(SpiralBound.HOUR_LENGTH), nextHour * 19.17f + 0.3f)
            hour = hourMesh
        }
        if (minuteMesh == null || nextMinute != minuteGen) {
            minuteGen = nextMinute
            minuteMesh = ClockHands.lightning(SpiralBound.capped(SpiralBound.MINUTE_LENGTH), nextMinute * 19.17f + 1.1f)
            minute = minuteMesh
        }
        if (secondMesh == null || nextSecond != secondGen) {
            secondGen = nextSecond
            secondMesh = ClockHands.lightning(SpiralBound.capped(SpiralBound.SECOND_LENGTH), nextSecond * 19.17f + 2.4f)
            second = secondMesh
        }
        return ClockHandsSet(hourMesh, minuteMesh, secondMesh)
    }
}

internal class ClockSceneState(
    val shader: ClockShaderHandles,
    val surfaceMesh: GlMesh,
    val classicBody: GlMesh,
    val classicHands: ClockHandsSet,
    val lightning: LightningBank,
    val backdrop: GlMesh,
    val pose: ClockPose = ClockPose(),
) {
    val view = FloatArray(MATRIX_FLOATS)
    val projection = FloatArray(MATRIX_FLOATS)
    private var daliBody: GlMesh? = null
    private var sphereBody: GlMesh? = null
    private var iceBody: GlMesh? = null
    private var iceShell: GlMesh? = null
    private var iceMist: GlMesh? = null
    private var daliHands: ClockHandsSet? = null
    private var iceHands: ClockHandsSet? = null
    private var daliHour: GlMesh? = null
    private var daliMinute: GlMesh? = null
    private var daliSecond: GlMesh? = null

    fun body(mode: ClockMode, seconds: Float): GlMesh = when (mode) {
        ClockMode.Classic -> classicBody
        ClockMode.Dali -> {
            val built = ClockMeshes.daliBody(seconds)
            val kept = reuseMesh(daliBody, built)
            daliBody = kept
            kept
        }
        ClockMode.Spheres -> sphereBody ?: ClockMeshes.sphereBody().also { sphereBody = it }
        ClockMode.Ice -> iceBody ?: ClockMeshes.iceBody().also { iceBody = it }
    }

    fun shell(mode: ClockMode): GlMesh? =
        if (mode == ClockMode.Ice) iceShell ?: ClockMeshes.iceShell().also { iceShell = it } else null

    fun mist(mode: ClockMode): GlMesh? =
        if (mode == ClockMode.Ice) iceMist ?: ClockMeshes.iceMist().also { iceMist = it } else null

    fun hands(mode: ClockMode, wave: Float, seconds: Float): ClockHandsSet = when (mode) {
        ClockMode.Classic -> classicHands
        ClockMode.Dali -> {
            val source = daliHands ?: ClockHandsSet(
                ClockHands.daliHour(),
                ClockHands.daliMinute(),
                ClockHands.daliSecond(),
            ).also { daliHands = it }
            ClockHandsSet(
                warpDaliHand(daliHour, source.hour, seconds).also { daliHour = it },
                warpDaliHand(daliMinute, source.minute, seconds).also { daliMinute = it },
                warpDaliHand(daliSecond, source.second, seconds).also { daliSecond = it },
            )
        }
        ClockMode.Spheres -> lightning.hands(wave)
        ClockMode.Ice -> iceHands ?: ClockHandsSet(
            ClockHands.iceHour(),
            ClockHands.iceMinute(),
            ClockHands.iceSecond(),
        ).also { iceHands = it }
    }
}

internal fun createClockScene(): ClockSceneState {
    // Surface mesh supplies clear color and drawable size; classic body is the same geometry.
    val classic = ClockMeshes.classicBody()
    return ClockSceneState(
        shader = buildClockShader(),
        surfaceMesh = classic,
        classicBody = classic,
        classicHands = ClockHandsSet(ClockHands.hour(), ClockHands.minute(), ClockHands.second()),
        lightning = LightningBank(),
        backdrop = ClockMeshes.backdrop(),
    )
}

internal fun GlFrame.renderClock(
    scene: ClockSceneState,
    mode: ClockMode,
    iceYaw: Float = 0.50f,
    classicSpinElapsed: Float = -1f,
) {
    val cal = Calendar.getInstance()
    val (hourAngle, minuteAngle, secondAngle) = ClockMath.handAngleRadians(
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE),
        cal.get(Calendar.SECOND),
        cal.get(Calendar.MILLISECOND),
    )
    val wave = seconds * LightningStrike.WAVE_RATE
    val radius = if (mode == ClockMode.Ice) 0.92f else 1.15f
    val halfW = if (aspect >= 1f) radius * aspect else radius
    val halfH = if (aspect >= 1f) radius else radius / aspect
    runtime.set(
        scene.shader.view,
        lookAt(0f, 0.04f, 4f, 0f, 0f, 0f, 0f, 1f, 0f, scene.view),
    )
    runtime.set(
        scene.shader.projection,
        ortho(-halfW, halfW, -halfH, halfH, 0.2f, 12f, scene.projection),
    )
    runtime.set(scene.shader.mode, mode.shaderValue)
    runtime.set(scene.shader.wave, wave)
    runtime.set(
        scene.shader.handReach,
        SpiralBound.capped(SpiralBound.HOUR_LENGTH),
        SpiralBound.capped(SpiralBound.MINUTE_LENGTH),
        SpiralBound.capped(SpiralBound.SECOND_LENGTH),
    )
    val (lx, ly, lz) = when (mode) {
        ClockMode.Classic -> Triple(0.25f, 0.55f, 0.90f)
        ClockMode.Dali -> Triple(0.30f, 0.50f, 0.88f)
        ClockMode.Spheres -> Triple(0.28f, 0.52f, 0.90f)
        ClockMode.Ice -> Triple(0.18f, 0.42f, 1.0f)
    }
    runtime.set(scene.shader.light, lx, ly, lz)

    val hands = scene.hands(mode, wave, seconds)
    scene.pose.update(mode, seconds, iceYaw, classicSpinElapsed, hourAngle, minuteAngle, secondAngle)
    val body = scene.body(mode, seconds)
    material = scene.shader.material
    draw(scene.backdrop, model = scene.pose.identity, material = ClockMaterial.Backdrop.uvX)
    draw(body, model = scene.pose.bodyWorld)
    draw(hands.hour, model = scene.pose.hourWorld, material = ClockMaterial.HourHand.uvX)
    draw(hands.minute, model = scene.pose.minuteWorld, material = ClockMaterial.MinuteHand.uvX)
    draw(hands.second, model = scene.pose.secondWorld, material = ClockMaterial.SecondHand.uvX)
    if (mode == ClockMode.Ice) {
        val hull = scene.shell(mode)
        val haze = scene.mist(mode)
        if (hull != null) {
            draw(
                hull,
                model = scene.pose.bodyWorld,
                pipeline = GlPipeline(
                    blend = true,
                    srcFactor = BlendFactor.SrcAlpha,
                    dstFactor = BlendFactor.One,
                    depthMask = false,
                ),
            )
        }
        if (haze != null) {
            draw(
                haze,
                model = scene.pose.identity,
                material = ClockMaterial.Mist.uvX,
                pipeline = GlPipeline(
                    blend = true,
                    srcFactor = BlendFactor.SrcAlpha,
                    dstFactor = BlendFactor.OneMinusSrcAlpha,
                    depthMask = false,
                ),
            )
        }
    }
}
