package ru.redbyte.clock3d.gl

import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.box
import ru.redbyte.redbytefx.gl.compose.sphere
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Melted pocket watch after the reference: wide top, drip at 6,
 * continuous silver bezel, gold crown at 11.
 */
internal object DaliWatch {
    private val landmarks: List<Pair<Float, Float>> = listOf(
        -0.30f to 0.56f,
        -0.10f to 0.66f,
        0.16f to 0.67f,
        0.42f to 0.50f,
        0.64f to 0.26f,
        0.74f to -0.02f,
        0.66f to -0.22f,
        0.52f to -0.40f,
        0.38f to -0.56f,
        0.24f to -0.70f,
        0.10f to -0.80f,
        -0.02f to -0.84f,
        -0.14f to -0.80f,
        -0.26f to -0.68f,
        -0.36f to -0.52f,
        -0.44f to -0.32f,
        -0.50f to -0.12f,
        -0.52f to 0.16f,
        -0.44f to 0.40f,
        -0.34f to 0.54f,
    )

    /**
     * The Dali silhouette keeps pouring: the drip at 6 lengthens, the bezel shears,
     * the whole face warps. Slow enough to read as time, strong enough to deform.
     */
    fun flowInto(x: Float, y: Float, z: Float, seconds: Float, out: FloatArray) {
        val t = seconds * 0.64f
        val drip = ((-y) + 0.42f).coerceIn(0.22f, 1.20f)
        val along = atan2(x, y)
        val swell = sin(along * 1.15f - t * 0.82f)
        val pour = 0.5f + 0.5f * sin(t * 0.46f)
        val stretch = sin(t * 0.34f)
        out[0] = x +
            sin(t * 0.68f + y * 1.15f) * 0.12f * drip +
            swell * 0.09f * drip +
            y * sin(t * 0.38f) * 0.10f
        out[1] = y -
            drip * (0.04f + 0.10f * pour) +
            cos(t * 0.42f + x * 1.05f) * 0.08f +
            y * stretch * 0.16f
        out[2] = z + sin(t * 0.50f + along * 0.85f) * 0.040f * drip
    }

    fun body(seconds: Float = 0f): GlMesh {
        val outline = resample(flowingLandmarks(seconds), 192)
        return mergeMeshes(
            listOf(
                tagMaterial(slab(outline, 0.026f), ClockMaterial.Face),
                tagMaterial(tubeAlong(outline, radius = 0.024f, rings = 16, z = 0.02f), ClockMaterial.Rim),
                tagMaterial(fold(seconds), ClockMaterial.Face),
                tagMaterial(crown(outline), ClockMaterial.Accent),
                tagMaterial(numerals(seconds), ClockMaterial.Digit),
            ),
            clearR = 0.01f,
            clearG = 0.01f,
            clearB = 0.015f,
        )
    }

    private val flowScratch = FloatArray(3)

    private fun flowingLandmarks(seconds: Float): List<Pair<Float, Float>> =
        landmarks.map { (x, y) ->
            flowInto(x, y, 0f, seconds, flowScratch)
            flowScratch[0] to flowScratch[1]
        }

    private fun crown(outline: List<Pair<Float, Float>>): GlMesh {
        val (x, y) = outline.minBy { kotlin.math.abs(it.first + 0.32f) + kotlin.math.abs(it.second - 0.56f) }
        val pose = FloatArray(ClockMath.MATRIX_SIZE)
        val rot = FloatArray(ClockMath.MATRIX_SIZE)
        val trans = FloatArray(ClockMath.MATRIX_SIZE)
        ClockMath.translation(x, y, 0.055f, trans)
        ClockMath.rotationZ(-atan2(x, y), rot)
        ClockMath.multiply(trans, rot, pose)
        val local = FloatArray(ClockMath.MATRIX_SIZE)
        val bud = FloatArray(ClockMath.MATRIX_SIZE)
        ClockMath.translation(0f, 0.10f, 0.012f, local)
        ClockMath.multiply(pose, local, bud)
        return mergeMeshes(
            listOf(
                transformMesh(box(0f, 0.040f, 0f, 0.009f, 0.040f, 0.009f), pose),
                transformMesh(sphere(0.028f, stacks = 10, slices = 14), bud),
                transformMesh(box(0f, 0f, 0.024f, 0.028f, 0.005f, 0.005f), bud),
                transformMesh(box(0f, 0f, 0f, 0.005f, 0.005f, 0.026f), bud),
            ),
        )
    }

    private fun fold(seconds: Float): GlMesh {
        val pose = FloatArray(ClockMath.MATRIX_SIZE)
        val rot = FloatArray(ClockMath.MATRIX_SIZE)
        val trans = FloatArray(ClockMath.MATRIX_SIZE)
        flowInto(-0.02f, 0.02f, 0.033f, seconds, flowScratch)
        ClockMath.translation(flowScratch[0], flowScratch[1], flowScratch[2], trans)
        ClockMath.rotationZ(-0.55f, rot)
        ClockMath.multiply(trans, rot, pose)
        return transformMesh(box(0f, 0f, 0f, 0.28f, 0.009f, 0.005f), pose)
    }

    private fun numerals(seconds: Float): GlMesh {
        val parts = ArrayList<GlMesh>(12)
        fun place(hour: Int, scale: Float, asDot: Boolean) {
            val t = hour / 12f * (Math.PI * 2).toFloat()
            val px = sin(t) * 0.36f
            val py = cos(t) * 0.30f + 0.06f
            val drip = when (hour) {
                6 -> 0.00f to -0.62f
                5 -> 0.30f to -0.36f
                7 -> -0.26f to -0.30f
                4 -> 0.44f to -0.02f
                12 -> -0.04f to 0.46f
                1 -> 0.24f to 0.40f
                else -> px to py
            }
            flowInto(drip.first, drip.second, 0.06f, seconds, flowScratch)
            parts += if (asDot) {
                translateMesh(sphere(0.012f, stacks = 8, slices = 10), flowScratch[0], flowScratch[1], flowScratch[2])
            } else {
                translateMesh(ClockDigits.meshForHour(hour, scale), flowScratch[0], flowScratch[1], flowScratch[2])
            }
        }
        place(12, 0.016f, asDot = false)
        place(1, 0.014f, asDot = false)
        place(4, 0.014f, asDot = false)
        place(5, 0.014f, asDot = true)
        place(6, 0.014f, asDot = false)
        place(7, 0.014f, asDot = false)
        flowInto(0f, 0.12f, 0.06f, seconds, flowScratch)
        parts += translateMesh(box(0f, 0f, 0f, 0.014f, 0.014f, 0.012f), flowScratch[0], flowScratch[1], flowScratch[2])
        return mergeMeshes(parts)
    }

    private fun resample(points: List<Pair<Float, Float>>, count: Int): List<Pair<Float, Float>> {
        val closed = points + points.first()
        val out = ArrayList<Pair<Float, Float>>(count)
        for (i in 0 until count) {
            val t = i / count.toFloat() * (closed.size - 1)
            val i1 = t.toInt().coerceIn(0, closed.size - 2)
            val f = t - i1
            val i0 = (i1 - 1).coerceAtLeast(0)
            val i2 = (i1 + 1).coerceAtMost(closed.size - 1)
            val i3 = (i1 + 2).coerceAtMost(closed.size - 1)
            out += catmull(closed[i0], closed[i1], closed[i2], closed[i3], f)
        }
        return out
    }

    private fun catmull(
        p0: Pair<Float, Float>,
        p1: Pair<Float, Float>,
        p2: Pair<Float, Float>,
        p3: Pair<Float, Float>,
        t: Float,
    ): Pair<Float, Float> {
        val t2 = t * t
        val t3 = t2 * t
        fun axis(a: Float, b: Float, c: Float, d: Float): Float =
            0.5f * (2f * b + (-a + c) * t + (2f * a - 5f * b + 4f * c - d) * t2 + (-a + 3f * b - 3f * c + d) * t3)
        return axis(p0.first, p1.first, p2.first, p3.first) to
            axis(p0.second, p1.second, p2.second, p3.second)
    }
}
