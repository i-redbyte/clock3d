package ru.redbyte.clock3d.gl

import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.MESH_STRIDE
import ru.redbyte.redbytefx.gl.compose.merge
import ru.redbyte.redbytefx.gl.compose.tagUv
import ru.redbyte.redbytefx.gl.compose.transform
import ru.redbyte.redbytefx.scene.translation

internal fun translateMesh(mesh: GlMesh, dx: Float, dy: Float, dz: Float): GlMesh =
    transform(mesh, translation(dx, dy, dz))

internal fun transformMesh(
    mesh: GlMesh,
    matrix: FloatArray,
    material: ClockMaterial? = null,
): GlMesh {
    val moved = transform(mesh, matrix)
    return if (material == null) moved else tagUv(moved, material.uvX)
}

internal fun tagMaterial(mesh: GlMesh, material: ClockMaterial, layer: Float = 0f): GlMesh =
    tagUv(mesh, material.uvX, layer)

internal fun warpDaliVertices(template: FloatArray, seconds: Float, into: FloatArray) {
    require(into.size == template.size) { "Warp target must match template vertex count" }
    val flowed = FloatArray(3)
    var index = 0
    while (index < template.size) {
        DaliWatch.flowInto(template[index], template[index + 1], template[index + 2], seconds, flowed)
        into[index] = flowed[0]
        into[index + 1] = flowed[1]
        into[index + 2] = flowed[2]
        index += MESH_STRIDE
    }
}

/** Warps [template] into an existing mesh or allocates once when [slot] is null or incompatible. */
internal fun warpDaliHand(slot: GlMesh?, template: GlMesh, seconds: Float): GlMesh {
    val indicesMatch = slot != null && when {
        slot.indices === template.indices -> true
        slot.indices == null && template.indices == null -> true
        slot.indices != null && template.indices != null -> slot.indices.contentEquals(template.indices)
        else -> false
    }
    if (slot != null && slot.vertices.size == template.vertices.size &&
        slot.stride == template.stride && indicesMatch
    ) {
        warpDaliVertices(template.vertices, seconds, slot.vertices)
        slot.replace(slot.vertices)
        return slot
    }
    val out = template.vertices.copyOf()
    warpDaliVertices(template.vertices, seconds, out)
    return GlMesh(
        vertices = out,
        stride = template.stride,
        attribs = template.attribs,
        mode = template.mode,
        patchVertices = template.patchVertices,
        depth = template.depth,
        clearR = template.clearR,
        clearG = template.clearG,
        clearB = template.clearB,
        indices = template.indices,
    )
}

internal fun mergeMeshes(
    parts: List<GlMesh>,
    clearR: Float = 0.12f,
    clearG: Float = 0.14f,
    clearB: Float = 0.2f,
): GlMesh {
    val merged = merge(parts)
    return GlMesh(
        vertices = merged.vertices,
        stride = merged.stride,
        attribs = merged.attribs,
        mode = merged.mode,
        patchVertices = merged.patchVertices,
        depth = merged.depth,
        clearR = clearR,
        clearG = clearG,
        clearB = clearB,
        indices = merged.indices,
    )
}
