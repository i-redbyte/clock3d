package ru.redbyte.clock3d.gl

import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.box
import ru.redbyte.redbytefx.gl.compose.quad
import ru.redbyte.redbytefx.gl.compose.sphere
import ru.redbyte.redbytefx.gl.compose.torus
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

internal object ClockMeshes {
    fun backdrop(): GlMesh {
        val back = FloatArray(ClockMath.MATRIX_SIZE)
        ClockMath.translation(0f, 0f, -1.25f, back)
        return tagMaterial(transformMesh(quad(4.2f, 4.2f), back), ClockMaterial.Backdrop)
    }

    fun classicBody(): GlMesh {
        val bezelLift = FloatArray(ClockMath.MATRIX_SIZE)
        ClockMath.rotationX((Math.PI / 2).toFloat(), bezelLift)
        val bezel = transformMesh(torus(0.90f, 0.045f, majorSegments = 48, minorSegments = 10), bezelLift)
        val ticks = ArrayList<GlMesh>(62)
        val pose = FloatArray(ClockMath.MATRIX_SIZE)
        for (tick in 0 until 60) {
            val major = tick % 5 == 0
            val angle = tick / 60f * (Math.PI * 2).toFloat()
            val length = if (major) 0.07f else 0.034f
            val half = if (major) 0.012f else 0.006f
            val mark = box(0f, 0f, 0f, half, length * 0.5f, 0.012f)
            ClockMath.placeOnDial(angle, 0.78f, 0.055f, pose)
            ticks += transformMesh(mark, pose)
        }
        ticks += box(0f, 0f, 0.07f, 0.04f, 0.04f, 0.03f)
        return mergeMeshes(
            listOf(
                tagMaterial(disc(0.86f, 0.03f, segments = 48), ClockMaterial.Face),
                tagMaterial(bezel, ClockMaterial.Rim),
                tagMaterial(mergeMeshes(ticks), ClockMaterial.Accent),
                tagMaterial(ClockDigits.onRing(radius = 0.56f, z = 0.06f, cell = 0.024f), ClockMaterial.Digit),
            ),
            clearR = 0.02f,
            clearG = 0.05f,
            clearB = 0.04f,
        )
    }

    fun daliBody(seconds: Float = 0f): GlMesh = DaliWatch.body(seconds)

    fun iceBody(): GlMesh = IceCrystal.interior()

    fun iceShell(): GlMesh = IceCrystal.shell()

    fun iceMist(): GlMesh = IceCrystal.mist()

    fun sphereBody(): GlMesh {
        val beads = ArrayList<GlMesh>(140)
        val digits = ArrayList<GlMesh>(12)
        val turns = 1.7f
        val count = 88
        val twoPi = (Math.PI * 2).toFloat()
        val maxTheta = turns * twoPi
        val growth = 1.8f
        val hourSlots = Array(12) { -1 }
        val hourDist = FloatArray(12) { Float.MAX_VALUE }
        data class Bead(val x: Float, val y: Float, val z: Float, val r: Float, val index: Int)
        val spiral = ArrayList<Bead>(count)
        for (index in 0 until count) {
            val t = index / (count - 1).toFloat()
            val theta = t * maxTheta
            val u = (exp(growth * t) - 1f) / (exp(growth) - 1f)
            val r = 0.16f + 0.66f * u
            val x = sin(theta) * r
            val y = cos(theta) * r
            val z = (t - 0.5f) * 0.10f
            val radius = 0.018f + t * 0.055f
            spiral += Bead(x, y, z, radius, index)
            if (t > 0.45f) {
                val hour = ((theta / twoPi) % 1f * 12f).toInt().mod(12)
                val target = hour / 12f * twoPi
                var d = kotlin.math.abs(theta % twoPi - target)
                if (d > Math.PI) d = (twoPi - d.toFloat())
                if (d < hourDist[hour]) {
                    hourDist[hour] = d
                    hourSlots[hour] = index
                }
            }
        }
        for (bead in spiral) {
            beads += translateMesh(sphere(bead.r, stacks = 8, slices = 10), bead.x, bead.y, bead.z)
        }
        for (hour in 0 until 12) {
            val slot = hourSlots[hour]
            if (slot < 0) continue
            val bead = spiral[slot]
            val number = if (hour == 0) 12 else hour
            digits += translateMesh(
                ClockDigits.meshForHour(number, 0.012f),
                bead.x,
                bead.y,
                bead.z + bead.r + 0.012f,
            )
        }
        beads += sphere(0.045f, stacks = 10, slices = 12)
        return mergeMeshes(
            listOf(
                tagMaterial(mergeMeshes(beads), ClockMaterial.Face),
                tagMaterial(mergeMeshes(digits), ClockMaterial.Digit),
            ),
            clearR = 0.04f,
            clearG = 0.01f,
            clearB = 0.01f,
        )
    }
}

/**
 * One circular outer bound for the whole nautilus.
 * Hands may cross the inner coil; they must not poke past the outer silhouette.
 */
internal object SpiralBound {
    const val HOUR_LENGTH: Float = 0.40f
    const val MINUTE_LENGTH: Float = 0.56f
    const val SECOND_LENGTH: Float = 0.70f

    /** Outermost bead edge is ~0.89; keep a little air so tips stay inside. */
    const val OUTER: Float = 0.80f

    fun capped(desired: Float): Float = minOf(desired, OUTER)
}
