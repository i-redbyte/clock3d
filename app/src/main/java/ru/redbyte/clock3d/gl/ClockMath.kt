package ru.redbyte.clock3d.gl

import ru.redbyte.redbytefx.scene.MATRIX_FLOATS
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import ru.redbyte.redbytefx.scene.identity as sceneIdentity
import ru.redbyte.redbytefx.scene.multiply as sceneMultiply
import ru.redbyte.redbytefx.scene.rotationX as sceneRotationX
import ru.redbyte.redbytefx.scene.rotationY as sceneRotationY
import ru.redbyte.redbytefx.scene.rotationZ as sceneRotationZ
import ru.redbyte.redbytefx.scene.scale as sceneScale
import ru.redbyte.redbytefx.scene.translation as sceneTranslation

internal object ClockMath {
    const val MATRIX_SIZE: Int = MATRIX_FLOATS

    fun identity(out: FloatArray = FloatArray(MATRIX_SIZE)): FloatArray = sceneIdentity(out)

    fun multiply(a: FloatArray, b: FloatArray, out: FloatArray = FloatArray(MATRIX_SIZE)): FloatArray =
        sceneMultiply(a, b, out)

    fun translation(x: Float, y: Float, z: Float, out: FloatArray = FloatArray(MATRIX_SIZE)): FloatArray =
        sceneTranslation(x, y, z, out)

    fun scale(sx: Float, sy: Float, sz: Float, out: FloatArray = FloatArray(MATRIX_SIZE)): FloatArray =
        sceneScale(sx, sy, sz, out)

    fun rotationX(radians: Float, out: FloatArray = FloatArray(MATRIX_SIZE)): FloatArray = sceneRotationX(radians, out)

    /**
     * Column-major rotation around +Z (standard CCW matrix).
     * A hand built along +Y reaches 3 o'clock at [radians] = −π/2.
     */
    fun rotationZ(radians: Float, out: FloatArray = FloatArray(MATRIX_SIZE)): FloatArray = sceneRotationZ(radians, out)

    /**
     * Angles for a hand that already lies on +Y at noon.
     * 12:00 → 0, 3:00 → −π/2 so [rotationZ] sends +Y to +X.
     */
    fun handAngleRadians(hours: Int, minutes: Int, seconds: Int, millis: Int): Triple<Float, Float, Float> {
        val sec = seconds + millis / 1000f
        val min = minutes + sec / 60f
        val hour = (hours % 12) + min / 60f
        val twoPi = (Math.PI * 2).toFloat()
        return Triple(
            -hour / 12f * twoPi,
            -min / 60f * twoPi,
            -sec / 60f * twoPi,
        )
    }

    /** Rotate a +Y hand around Z, then lift it off the dial. */
    fun handPose(angleRad: Float, liftZ: Float, out: FloatArray = FloatArray(MATRIX_SIZE)): FloatArray {
        val t = FloatArray(MATRIX_SIZE)
        val r = FloatArray(MATRIX_SIZE)
        translation(0f, 0f, liftZ, t)
        rotationZ(angleRad, r)
        return multiply(t, r, out)
    }

    fun rotationY(radians: Float, out: FloatArray = FloatArray(MATRIX_SIZE)): FloatArray = sceneRotationY(radians, out)

    /** Gentle hover: small XY drift and yaw, no big leaps. */
    fun hoverPose(seconds: Float, out: FloatArray = FloatArray(MATRIX_SIZE)): FloatArray {
        val t = FloatArray(MATRIX_SIZE)
        val y = FloatArray(MATRIX_SIZE)
        translation(
            sin(seconds * 0.42f) * 0.018f,
            sin(seconds * 0.63f) * 0.032f,
            0f,
            t,
        )
        rotationY(sin(seconds * 0.31f) * 0.07f, y)
        return multiply(t, y, out)
    }

    /**
     * Coin-on-edge spin around +Y: full speed at [elapsed] = 0, eases to rest facing the camera.
     * Lands on a whole number of turns so the dial returns to the start pose.
     */
    fun classicSpinYaw(elapsed: Float): Float {
        if (elapsed < 0f) return 0f
        val turns = 6f
        val tau = 1.25f
        if (elapsed > tau * 7f) return 0f
        return turns * 2f * Math.PI.toFloat() * (1f - exp(-elapsed / tau))
    }

    fun placeOnDial(
        angleFromTwelve: Float,
        radius: Float,
        z: Float,
        out: FloatArray = FloatArray(MATRIX_SIZE),
    ): FloatArray {
        val t = FloatArray(MATRIX_SIZE)
        val r = FloatArray(MATRIX_SIZE)
        translation(sin(angleFromTwelve) * radius, cos(angleFromTwelve) * radius, z, t)
        rotationZ(-angleFromTwelve, r)
        return multiply(t, r, out)
    }
}
