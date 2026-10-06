package ru.redbyte.clock3d.gl

import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.box

/** Raised block digits (1 … 12). */
internal object ClockDigits {
    private val patterns: Map<Int, List<Pair<Int, Int>>> = mapOf(
        0 to listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0),
            Pair(0, 1), Pair(2, 1),
            Pair(0, 2), Pair(2, 2),
            Pair(0, 3), Pair(2, 3),
            Pair(0, 4), Pair(1, 4), Pair(2, 4),
        ),
        1 to listOf(Pair(1, 0), Pair(1, 1), Pair(1, 2), Pair(1, 3), Pair(1, 4)),
        2 to listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0),
            Pair(2, 1),
            Pair(0, 2), Pair(1, 2), Pair(2, 2),
            Pair(0, 3),
            Pair(0, 4), Pair(1, 4), Pair(2, 4),
        ),
        3 to listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0),
            Pair(2, 1),
            Pair(0, 2), Pair(1, 2), Pair(2, 2),
            Pair(2, 3),
            Pair(0, 4), Pair(1, 4), Pair(2, 4),
        ),
        4 to listOf(
            Pair(0, 0), Pair(2, 0),
            Pair(0, 1), Pair(2, 1),
            Pair(0, 2), Pair(1, 2), Pair(2, 2),
            Pair(2, 3),
            Pair(2, 4),
        ),
        5 to listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0),
            Pair(0, 1),
            Pair(0, 2), Pair(1, 2), Pair(2, 2),
            Pair(2, 3),
            Pair(0, 4), Pair(1, 4), Pair(2, 4),
        ),
        6 to listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0),
            Pair(0, 1),
            Pair(0, 2), Pair(1, 2), Pair(2, 2),
            Pair(0, 3), Pair(2, 3),
            Pair(0, 4), Pair(1, 4), Pair(2, 4),
        ),
        7 to listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0),
            Pair(2, 1),
            Pair(2, 2),
            Pair(2, 3),
            Pair(2, 4),
        ),
        8 to listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0),
            Pair(0, 1), Pair(2, 1),
            Pair(0, 2), Pair(1, 2), Pair(2, 2),
            Pair(0, 3), Pair(2, 3),
            Pair(0, 4), Pair(1, 4), Pair(2, 4),
        ),
        9 to listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0),
            Pair(0, 1), Pair(2, 1),
            Pair(0, 2), Pair(1, 2), Pair(2, 2),
            Pair(2, 3),
            Pair(0, 4), Pair(1, 4), Pair(2, 4),
        ),
    )

    fun meshForDigit(digit: Int, cell: Float): GlMesh {
        val cells = patterns[digit] ?: return box(0f, 0f, 0f, cell * 0.3f, cell * 0.3f, cell * 0.3f)
        val gap = cell * 0.18f
        val cols = cells.maxOf { it.first } + 1
        val rows = 5
        val width = cols * cell + (cols - 1) * gap
        val height = rows * cell + (rows - 1) * gap
        val half = cell * 0.42f
        val blocks = cells.map { (col, row) ->
            val cx = -width * 0.5f + col * (cell + gap) + cell * 0.5f
            val cy = height * 0.5f - row * (cell + gap) - cell * 0.5f
            box(cx, cy, 0f, half, half, cell * 0.35f)
        }
        return mergeMeshes(blocks)
    }

    fun meshForHour(hour: Int, cell: Float): GlMesh {
        val shift = cell * 1.7f
        return when (hour) {
            10 -> mergeMeshes(
                listOf(
                    translateMesh(meshForDigit(1, cell), -shift, 0f, 0f),
                    translateMesh(meshForDigit(0, cell), shift, 0f, 0f),
                ),
            )
            11 -> mergeMeshes(
                listOf(
                    translateMesh(meshForDigit(1, cell), -shift, 0f, 0f),
                    translateMesh(meshForDigit(1, cell), shift, 0f, 0f),
                ),
            )
            12 -> mergeMeshes(
                listOf(
                    translateMesh(meshForDigit(1, cell), -shift, 0f, 0f),
                    translateMesh(meshForDigit(2, cell), shift, 0f, 0f),
                ),
            )
            else -> meshForDigit(hour, cell)
        }
    }

    fun onRing(radius: Float, z: Float, cell: Float): GlMesh {
        val parts = ArrayList<GlMesh>(12)
        val matrix = FloatArray(ClockMath.MATRIX_SIZE)
        for (hour in 1..12) {
            val angle = hour / 12f * (Math.PI * 2).toFloat()
            ClockMath.translation(kotlin.math.sin(angle) * radius, kotlin.math.cos(angle) * radius, z, matrix)
            parts += transformMesh(meshForHour(hour, cell), matrix)
        }
        return mergeMeshes(parts)
    }
}
