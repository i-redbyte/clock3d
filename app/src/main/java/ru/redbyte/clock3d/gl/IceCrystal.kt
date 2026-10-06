package ru.redbyte.clock3d.gl

import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.MESH_STRIDE
import ru.redbyte.redbytefx.gl.compose.box
import ru.redbyte.redbytefx.gl.compose.meshAttribs
import ru.redbyte.redbytefx.gl.compose.sphere
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Standing quartz: hexagonal prism along +Y, pyramids at both ends.
 * Cross-section lives in XZ, so the camera looks *through* the facets.
 * [shell] is the glass hull; [interior] is the frozen dial and the inner lattice.
 */
internal object IceCrystal {
    private val hexR = floatArrayOf(0.46f, 0.50f, 0.47f, 0.44f, 0.49f, 0.45f)

    fun interior(): GlMesh = mergeMeshes(
        listOf(
            tagMaterial(edges(scale = 0.985f, radius = 0.011f), ClockMaterial.Rim),
            tagMaterial(edges(scale = 0.42f, radius = 0.0045f), ClockMaterial.Accent),
            tagMaterial(hub(), ClockMaterial.Rim),
            tagMaterial(cracks(), ClockMaterial.Accent),
            tagMaterial(inclusions(), ClockMaterial.Rim),
            tagMaterial(disc(0.27f, 0.004f, segments = 24), ClockMaterial.Accent),
            tagMaterial(dialRing(), ClockMaterial.Rim),
            tagMaterial(ticks(), ClockMaterial.Accent),
            tagMaterial(ClockDigits.onRing(radius = 0.26f, z = 0.06f, cell = 0.040f), ClockMaterial.Digit),
        ),
        clearR = 0.01f,
        clearG = 0.03f,
        clearB = 0.06f,
    )

    fun shell(): GlMesh = tagMaterial(hull(), ClockMaterial.Face)

    fun mist(): GlMesh = mergeMeshes(
        listOf(
            tagMaterial(
                translateMesh(disc(1.88f, 0.006f, segments = 36), 0f, 0.02f, -0.38f),
                ClockMaterial.Mist,
                0.25f,
            ),
            tagMaterial(translateMesh(disc(2.00f, 0.006f, segments = 36), 0f, 0f, 0.28f), ClockMaterial.Mist, 0.7f),
        ),
    )

    private fun hull(): GlMesh {
        val builder = FacetMesh()
        hullFaces(quartz(1f), builder)
        return builder.mesh()
    }

    private fun edges(scale: Float, radius: Float): GlMesh {
        val builder = FacetMesh()
        hullEdges(quartz(scale), builder, radius)
        return builder.mesh()
    }

    private class Quartz(
        val top: Array<FloatArray>,
        val bot: Array<FloatArray>,
        val tip: FloatArray,
        val pit: FloatArray,
    )

    private fun quartz(scale: Float): Quartz {
        val top = Array(6) { i ->
            val a = i * (Math.PI / 3.0).toFloat()
            floatArrayOf(sin(a) * hexR[i] * scale, 0.34f * scale, cos(a) * hexR[i] * scale)
        }
        val bot = Array(6) { i ->
            val a = i * (Math.PI / 3.0).toFloat()
            val r = hexR[i] * 0.86f * scale
            floatArrayOf(sin(a) * r, -0.36f * scale, cos(a) * r)
        }
        return Quartz(
            top = top,
            bot = bot,
            tip = floatArrayOf(0.016f * scale, 0.96f * scale, 0.024f * scale),
            pit = floatArrayOf(0.010f * scale, -0.78f * scale, -0.014f * scale),
        )
    }

    private fun hullFaces(q: Quartz, builder: FacetMesh) {
        for (i in 0 until 6) {
            val n = (i + 1) % 6
            builder.quad(q.top[i], q.top[n], q.bot[n], q.bot[i])
            builder.tri(q.tip, q.top[i], q.top[n])
            builder.tri(q.pit, q.bot[n], q.bot[i])
        }
    }

    private fun hullEdges(q: Quartz, builder: FacetMesh, radius: Float) {
        for (i in 0 until 6) {
            val n = (i + 1) % 6
            builder.ridge(q.top[i], q.top[n], radius)
            builder.ridge(q.bot[i], q.bot[n], radius)
            builder.ridge(q.top[i], q.bot[i], radius)
            builder.ridge(q.tip, q.top[i], radius)
            builder.ridge(q.pit, q.bot[i], radius)
        }
    }

    private fun hub(): GlMesh = sphere(0.028f, stacks = 10, slices = 12)

    private fun cracks(): GlMesh {
        val builder = FacetMesh()
        builder.quad(
            floatArrayOf(-0.06f, 0.52f, -0.08f),
            floatArrayOf(0.10f, 0.78f, 0.06f),
            floatArrayOf(0.12f, 0.80f, -0.04f),
            floatArrayOf(-0.04f, 0.54f, 0.06f),
        )
        builder.quad(
            floatArrayOf(0.08f, -0.48f, 0.10f),
            floatArrayOf(-0.06f, -0.70f, 0.02f),
            floatArrayOf(-0.04f, -0.68f, -0.10f),
            floatArrayOf(0.10f, -0.46f, -0.06f),
        )
        return builder.mesh()
    }

    private fun inclusions(): GlMesh = mergeMeshes(
        listOf(
            translateMesh(sphere(0.022f, stacks = 8, slices = 10), 0.11f, 0.14f, 0.08f),
            translateMesh(sphere(0.016f, stacks = 8, slices = 10), -0.10f, -0.08f, -0.07f),
            translateMesh(sphere(0.012f, stacks = 8, slices = 10), 0.05f, -0.18f, 0.10f),
            translateMesh(sphere(0.010f, stacks = 8, slices = 10), -0.06f, 0.20f, -0.09f),
        ),
    )

    private fun dialRing(): GlMesh {
        val builder = FacetMesh()
        val radius = 0.30f
        val pts = Array(6) { i ->
            val a = i * (Math.PI / 3.0).toFloat()
            floatArrayOf(sin(a) * radius, cos(a) * radius, 0f)
        }
        for (i in 0 until 6) {
            builder.ridge(pts[i], pts[(i + 1) % 6], 0.0048f)
        }
        return builder.mesh()
    }

    private fun ticks(): GlMesh {
        val parts = ArrayList<GlMesh>(13)
        val pose = FloatArray(ClockMath.MATRIX_SIZE)
        for (hour in 0 until 12) {
            val angle = hour / 12f * (Math.PI * 2).toFloat()
            val major = hour % 3 == 0
            val mark = box(
                0f,
                0f,
                0f,
                if (major) 0.011f else 0.0055f,
                if (major) 0.034f else 0.020f,
                0.010f,
            )
            ClockMath.placeOnDial(angle, 0.30f, 0.05f, pose)
            parts += transformMesh(mark, pose)
        }
        return mergeMeshes(parts)
    }
}

private class FacetMesh {
    private val vertices = ArrayList<Float>()
    private val indices = ArrayList<Int>()
    private var faces = 0

    fun tri(a: FloatArray, b: FloatArray, c: FloatArray) {
        val ux = b[0] - a[0]
        val uy = b[1] - a[1]
        val uz = b[2] - a[2]
        val vx = c[0] - a[0]
        val vy = c[1] - a[1]
        val vz = c[2] - a[2]
        var nx = uy * vz - uz * vy
        var ny = uz * vx - ux * vz
        var nz = ux * vy - uy * vx
        val len = sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toFloat().coerceAtLeast(1e-6f)
        nx /= len
        ny /= len
        nz /= len
        val tint = (faces % 6) / 6f
        faces += 1
        val i0 = vertex(a[0], a[1], a[2], nx, ny, nz, tint)
        val i1 = vertex(b[0], b[1], b[2], nx, ny, nz, tint)
        val i2 = vertex(c[0], c[1], c[2], nx, ny, nz, tint)
        indices += i0
        indices += i1
        indices += i2
    }

    fun quad(a: FloatArray, b: FloatArray, c: FloatArray, d: FloatArray) {
        tri(a, b, c)
        tri(a, c, d)
    }

    fun ridge(a: FloatArray, b: FloatArray, radius: Float) {
        val dx = b[0] - a[0]
        val dy = b[1] - a[1]
        val dz = b[2] - a[2]
        val len = sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat().coerceAtLeast(1e-6f)
        val ux = dx / len
        val uy = dy / len
        val uz = dz / len
        val hx = if (abs(uy) < 0.85f) 0f else 1f
        val hy = if (abs(uy) < 0.85f) 1f else 0f
        var px = uy * 0f - uz * hy
        var py = uz * hx - ux * 0f
        var pz = ux * hy - uy * hx
        val pl = sqrt((px * px + py * py + pz * pz).toDouble()).toFloat().coerceAtLeast(1e-6f)
        px /= pl
        py /= pl
        pz /= pl
        var qx = uy * pz - uz * py
        var qy = uz * px - ux * pz
        var qz = ux * py - uy * px
        val ql = sqrt((qx * qx + qy * qy + qz * qz).toDouble()).toFloat().coerceAtLeast(1e-6f)
        qx /= ql
        qy /= ql
        qz /= ql
        fun corner(base: FloatArray, s: Float, t: Float): FloatArray = floatArrayOf(
            base[0] + (px * s + qx * t) * radius,
            base[1] + (py * s + qy * t) * radius,
            base[2] + (pz * s + qz * t) * radius,
        )
        val a0 = corner(a, 1f, 1f)
        val a1 = corner(a, -1f, 1f)
        val a2 = corner(a, -1f, -1f)
        val a3 = corner(a, 1f, -1f)
        val b0 = corner(b, 1f, 1f)
        val b1 = corner(b, -1f, 1f)
        val b2 = corner(b, -1f, -1f)
        val b3 = corner(b, 1f, -1f)
        quad(a0, a1, b1, b0)
        quad(a1, a2, b2, b1)
        quad(a2, a3, b3, b2)
        quad(a3, a0, b0, b3)
    }

    private fun vertex(
        x: Float,
        y: Float,
        z: Float,
        nx: Float,
        ny: Float,
        nz: Float,
        tint: Float,
    ): Int {
        val id = vertices.size / MESH_STRIDE
        vertices += x
        vertices += y
        vertices += z
        vertices += nx
        vertices += ny
        vertices += nz
        vertices += 0.5f
        vertices += tint
        return id
    }

    fun mesh(): GlMesh = GlMesh(
        vertices = vertices.toFloatArray(),
        stride = MESH_STRIDE,
        attribs = meshAttribs(),
        indices = indices.toIntArray(),
        depth = true,
    )
}
