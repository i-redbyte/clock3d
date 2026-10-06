package ru.redbyte.clock3d.gl

import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.scene.identity
import ru.redbyte.redbytefx.scene.multiply
import ru.redbyte.redbytefx.scene.rotationX
import ru.redbyte.redbytefx.scene.rotationY
import ru.redbyte.redbytefx.scene.rotationZ

internal class ClockPose {
    val bodyWorld: FloatArray = FloatArray(ClockMath.MATRIX_SIZE)
    val hourWorld: FloatArray = FloatArray(ClockMath.MATRIX_SIZE)
    val minuteWorld: FloatArray = FloatArray(ClockMath.MATRIX_SIZE)
    val secondWorld: FloatArray = FloatArray(ClockMath.MATRIX_SIZE)
    val identity: FloatArray = identity()

    private val tilt = FloatArray(ClockMath.MATRIX_SIZE)
    private val iceTilt = FloatArray(ClockMath.MATRIX_SIZE)
    private val hover = FloatArray(ClockMath.MATRIX_SIZE)
    private val spin = FloatArray(ClockMath.MATRIX_SIZE)
    private val pose = FloatArray(ClockMath.MATRIX_SIZE)

    init {
        rotationX(-0.18f, tilt)
        rotationX(-0.40f, iceTilt)
    }

    fun update(
        mode: ClockMode,
        seconds: Float,
        iceYaw: Float,
        classicSpinElapsed: Float,
        hourAngle: Float,
        minuteAngle: Float,
        secondAngle: Float,
    ) {
        when (mode) {
            ClockMode.Dali -> {
                ClockMath.hoverPose(seconds, hover)
                multiply(hover, tilt, bodyWorld)
            }
            ClockMode.Spheres -> {
                rotationZ(-seconds * 0.16f, spin)
                multiply(tilt, spin, bodyWorld)
            }
            ClockMode.Ice -> {
                rotationY(iceYaw, spin)
                multiply(iceTilt, spin, bodyWorld)
            }
            ClockMode.Classic -> {
                rotationY(ClockMath.classicSpinYaw(classicSpinElapsed), spin)
                multiply(spin, tilt, bodyWorld)
            }
        }
        val lifts = when (mode) {
            ClockMode.Classic -> Triple(0.08f, 0.075f, 0.09f)
            ClockMode.Dali -> Triple(0.07f, 0.065f, 0.08f)
            ClockMode.Spheres -> Triple(0.13f, 0.12f, 0.15f)
            ClockMode.Ice -> Triple(0.075f, 0.070f, 0.088f)
        }
        handWorld(hourAngle, lifts.first, hourWorld)
        handWorld(minuteAngle, lifts.second, minuteWorld)
        handWorld(secondAngle, lifts.third, secondWorld)
    }

    private fun handWorld(angle: Float, lift: Float, out: FloatArray) {
        ClockMath.handPose(angle, lift, pose)
        multiply(bodyWorld, pose, out)
    }
}

internal fun reuseMesh(slot: GlMesh?, built: GlMesh): GlMesh {
    if (slot == null) return built
    val indices = built.indices
    if (indices != null) slot.replace(built.vertices, indices) else slot.replace(built.vertices)
    return slot
}
