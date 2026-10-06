package ru.redbyte.clock3d.gl

import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.box
import ru.redbyte.redbytefx.gl.compose.sphere
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

internal object ClockHands {
    fun hour(): GlMesh = spear(length = 0.40f, hub = 0.038f, halfZ = 0.010f)

    fun minute(): GlMesh = spear(length = 0.58f, hub = 0.026f, halfZ = 0.008f)

    fun second(): GlMesh = spear(length = 0.70f, hub = 0.012f, halfZ = 0.005f)

    fun iceHour(): GlMesh = iceNeedle(length = 0.20f, radius = 0.011f)

    fun iceMinute(): GlMesh = iceNeedle(length = 0.27f, radius = 0.0060f)

    fun iceSecond(): GlMesh = iceNeedle(length = 0.32f, radius = 0.0030f)

    private fun iceNeedle(length: Float, radius: Float): GlMesh = mergeMeshes(
        listOf(
            box(0f, length * 0.48f, 0f, radius, length * 0.48f, radius * 0.55f),
            box(0f, length * 0.82f, 0f, radius * 0.55f, length * 0.16f, radius * 0.35f),
            translateMesh(sphere(radius * 1.15f, stacks = 8, slices = 10), 0f, length, 0f),
            sphere(radius * 2.1f, stacks = 8, slices = 10),
        ),
    )

    fun daliHour(): GlMesh = needle(length = 0.26f, radius = 0.0040f, bend = 0.012f)

    fun daliMinute(): GlMesh = needle(length = 0.40f, radius = 0.0030f, bend = 0.018f)

    fun daliSecond(): GlMesh = needle(length = 0.44f, radius = 0.0018f, bend = 0.022f)

    private fun needle(length: Float, radius: Float, bend: Float): GlMesh {
        val mid = length * 0.55f
        val pose = FloatArray(ClockMath.MATRIX_SIZE)
        val rot = FloatArray(ClockMath.MATRIX_SIZE)
        val trans = FloatArray(ClockMath.MATRIX_SIZE)
        ClockMath.translation(bend * 0.45f, mid + (length - mid) * 0.5f, 0f, trans)
        ClockMath.rotationZ(-0.18f, rot)
        ClockMath.multiply(trans, rot, pose)
        return mergeMeshes(
            listOf(
                box(0f, mid * 0.5f, 0f, radius, mid * 0.5f, radius),
                transformMesh(box(0f, 0f, 0f, radius * 0.85f, (length - mid) * 0.5f, radius * 0.85f), pose),
                translateMesh(sphere(radius * 1.5f, stacks = 8, slices = 10), bend, length, 0f),
                sphere(radius * 2.4f, stacks = 8, slices = 10),
            ),
        )
    }

    fun lightning(length: Float, seed: Float): GlMesh {
        val tipX = (hash(seed) - 0.5f) * length * 0.08f
        val spread = 0.048f + hash(seed + 1.7f) * 0.055f
        val depth = if (hash(seed + 3.3f) > 0.55f) 4 else 3
        val main = fractalBolt(0f, 0f, tipX, length, seed, depth, spread)
        val parts = ArrayList<GlMesh>(96)
        val core = 0.0018f + hash(seed + 4.9f) * 0.0007f
        val glow = core * (1.7f + hash(seed + 6.1f) * 0.4f)
        stroke(main, core, glow, parts)
        val forks = 1 + (hash(seed + 8.8f) * 2.4f).toInt()
        for (fork in 0 until forks) {
            val along = 0.32f + hash(seed + 11.3f + fork * 3.1f) * 0.50f
            val scale = 0.10f + hash(seed + 14.7f + fork * 2.4f) * 0.20f
            branch(main, length, seed + 17.9f + fork * 5.7f, along, scale, parts)
        }
        parts += sphere(0.0075f + hash(seed + 21f) * 0.0025f, stacks = 8, slices = 10)
        return mergeMeshes(parts)
    }

    private fun branch(
        main: List<Pair<Float, Float>>,
        length: Float,
        seed: Float,
        along: Float,
        scale: Float,
        parts: ArrayList<GlMesh>,
    ) {
        val index = ((main.size - 1) * along).toInt().coerceIn(1, main.size - 2)
        val origin = main[index]
        val prev = main[index - 1]
        val dx = origin.first - prev.first
        val dy = origin.second - prev.second
        val side = if (hash(seed) > 0.5f) 1f else -1f
        val kick = (0.45f + hash(seed + 0.7f) * 0.85f) * side
        val angle = atan2(dx, dy) + kick
        val reach = length * scale
        val path = fractalBolt(
            origin.first,
            origin.second,
            origin.first + sin(angle) * reach,
            origin.second + cos(angle) * reach,
            seed,
            depth = 2 + (hash(seed + 1.4f) * 2f).toInt(),
            spread = 0.045f + hash(seed + 2.2f) * 0.05f,
        )
        stroke(path, core = 0.0011f + hash(seed + 3.6f) * 0.0005f, glow = 0.0022f + hash(seed + 4.4f) * 0.0008f, parts)
        if (hash(seed + 5.5f) > 0.62f) {
            val mid = path[(path.size * 0.55f).toInt().coerceIn(1, path.size - 2)]
            val twigAngle = angle + side * (0.5f + hash(seed + 6.2f) * 0.6f)
            val twig = length * scale * (0.35f + hash(seed + 7.1f) * 0.25f)
            stroke(
                fractalBolt(
                    mid.first,
                    mid.second,
                    mid.first + sin(twigAngle) * twig,
                    mid.second + cos(twigAngle) * twig,
                    seed + 9.4f,
                    depth = 2,
                    spread = 0.04f,
                ),
                core = 0.0009f,
                glow = 0.0018f,
                parts,
            )
        }
    }

    private fun hash(seed: Float): Float {
        val raw = sin(seed * 127.1f + 311.7f) * 43758.5453f
        return raw - kotlin.math.floor(raw.toDouble()).toFloat()
    }

    private fun fractalBolt(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        seed: Float,
        depth: Int,
        spread: Float,
    ): List<Pair<Float, Float>> {
        var points = listOf(x0 to y0, x1 to y1)
        var amp = spread
        var salt = seed
        repeat(depth) {
            val next = ArrayList<Pair<Float, Float>>(points.size * 2)
            for (i in 0 until points.size - 1) {
                val a = points[i]
                val b = points[i + 1]
                val mx = (a.first + b.first) * 0.5f
                val my = (a.second + b.second) * 0.5f
                val dx = b.first - a.first
                val dy = b.second - a.second
                val len = hypot(dx, dy).coerceAtLeast(1e-4f)
                val nx = -dy / len
                val ny = dx / len
                salt = sin(salt * 12.9898f + i * 78.233f) * 43758.55f
                val jitter = (salt - kotlin.math.floor(salt.toDouble()).toFloat()) * 2f - 1f
                next += a
                next += (mx + nx * jitter * amp) to (my + ny * jitter * amp)
            }
            next += points.last()
            points = next
            amp *= 0.52f
        }
        return points
    }

    private fun stroke(
        path: List<Pair<Float, Float>>,
        core: Float,
        glow: Float,
        parts: ArrayList<GlMesh>,
    ) {
        val pose = FloatArray(ClockMath.MATRIX_SIZE)
        val rot = FloatArray(ClockMath.MATRIX_SIZE)
        val trans = FloatArray(ClockMath.MATRIX_SIZE)
        for (i in 0 until path.size - 1) {
            val a = path[i]
            val b = path[i + 1]
            val dx = b.first - a.first
            val dy = b.second - a.second
            val seg = hypot(dx, dy).coerceAtLeast(0.001f)
            ClockMath.rotationZ(-atan2(dx, dy), rot)
            ClockMath.translation((a.first + b.first) * 0.5f, (a.second + b.second) * 0.5f, 0f, trans)
            ClockMath.multiply(trans, rot, pose)
            parts += tagMaterial(
                transformMesh(box(0f, 0f, 0f, glow, seg * 0.52f, glow * 0.22f), pose),
                ClockMaterial.Accent,
                layer = 1f,
            )
            parts += transformMesh(box(0f, 0f, 0f, core, seg * 0.52f, core * 0.28f), pose)
        }
    }
}
