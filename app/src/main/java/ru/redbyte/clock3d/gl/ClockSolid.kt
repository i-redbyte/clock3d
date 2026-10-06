package ru.redbyte.clock3d.gl

import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.MESH_STRIDE
import ru.redbyte.redbytefx.gl.compose.extrudePolygon
import ru.redbyte.redbytefx.gl.compose.meshAttribs
import ru.redbyte.redbytefx.gl.compose.disc as sceneDisc
import ru.redbyte.redbytefx.gl.compose.tubeAlong as sceneTube

/** Filled disc in XY, facing ±Z, with a short rim. */
internal fun disc(radius: Float, halfZ: Float, segments: Int = 48): GlMesh = sceneDisc(radius, halfZ, segments)

/** Filled polygon in XY from a closed outline, with thickness. */
internal fun slab(outline: List<Pair<Float, Float>>, halfZ: Float): GlMesh = extrudePolygon(outline, halfZ)

/** Smooth closed tube along an XY path. */
internal fun tubeAlong(
    path: List<Pair<Float, Float>>,
    radius: Float,
    rings: Int = 8,
    z: Float = 0.05f,
): GlMesh = sceneTube(path, radius, rings, z)

/**
 * Flattened spear along +Y: wide at the hub, pointed tip, small counterweight.
 */
internal fun spear(length: Float, hub: Float, halfZ: Float): GlMesh {
    val vertices = ArrayList<Float>()
    val indices = ArrayList<Int>()
    fun vertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float): Int {
        val id = vertices.size / MESH_STRIDE
        vertices += x
        vertices += y
        vertices += z
        vertices += nx
        vertices += ny
        vertices += nz
        vertices += 0.5f
        vertices += 0.5f
        return id
    }
    fun tri(a: Int, b: Int, c: Int) {
        indices += a
        indices += b
        indices += c
    }
    fun face(
        ax: Float,
        ay: Float,
        az: Float,
        bx: Float,
        by: Float,
        bz: Float,
        cx: Float,
        cy: Float,
        cz: Float,
    ) {
        val ux = bx - ax
        val uy = by - ay
        val uz = bz - az
        val vx = cx - ax
        val vy = cy - ay
        val vz = cz - az
        var nx = uy * vz - uz * vy
        var ny = uz * vx - ux * vz
        var nz = ux * vy - uy * vx
        val len = kotlin.math.sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toFloat().coerceAtLeast(1e-6f)
        nx /= len
        ny /= len
        nz /= len
        tri(vertex(ax, ay, az, nx, ny, nz), vertex(bx, by, bz, nx, ny, nz), vertex(cx, cy, cz, nx, ny, nz))
    }
    val hubLift = 0.02f
    face(-hub, hubLift, halfZ, hub, hubLift, halfZ, 0f, length, halfZ)
    face(hub, hubLift, -halfZ, -hub, hubLift, -halfZ, 0f, length, -halfZ)
    face(-hub, hubLift, halfZ, 0f, length, halfZ, 0f, length, -halfZ)
    face(-hub, hubLift, halfZ, 0f, length, -halfZ, -hub, hubLift, -halfZ)
    face(0f, length, halfZ, hub, hubLift, halfZ, hub, hubLift, -halfZ)
    face(0f, length, halfZ, hub, hubLift, -halfZ, 0f, length, -halfZ)
    face(-hub, hubLift, -halfZ, hub, hubLift, -halfZ, hub, hubLift, halfZ)
    face(-hub, hubLift, -halfZ, hub, hubLift, halfZ, -hub, hubLift, halfZ)
    val tail = hub * 1.7f
    face(-hub * 0.55f, hubLift, halfZ, 0f, -tail, halfZ, hub * 0.55f, hubLift, halfZ)
    face(hub * 0.55f, hubLift, -halfZ, 0f, -tail, -halfZ, -hub * 0.55f, hubLift, -halfZ)
    return GlMesh(
        vertices = vertices.toFloatArray(),
        stride = MESH_STRIDE,
        attribs = meshAttribs(),
        indices = indices.toIntArray(),
        depth = true,
    )
}
