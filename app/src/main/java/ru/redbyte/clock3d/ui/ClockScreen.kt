package ru.redbyte.clock3d.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.redbyte.clock3d.R
import ru.redbyte.clock3d.gl.ClockMode
import ru.redbyte.clock3d.gl.createClockScene
import ru.redbyte.clock3d.gl.renderClock
import ru.redbyte.redbytefx.gl.compose.GL_LINK_FALLBACK
import ru.redbyte.redbytefx.gl.compose.GlLinkErrorOverlay
import ru.redbyte.redbytefx.gl.compose.GlLinkState
import ru.redbyte.redbytefx.gl.compose.GlSurface
import ru.redbyte.redbytefx.gl.compose.GlSurfaceConfig
import ru.redbyte.redbytefx.gl.compose.rememberGlController
import java.util.concurrent.atomic.AtomicLong

@Composable
fun ClockScreen(initialMode: ClockMode = ClockMode.Classic) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var iceYaw by remember { mutableFloatStateOf(0.50f) }
    val classicSpinAt = remember { AtomicLong(-1L) }
    val scene = remember { createClockScene() }
    val controller = rememberGlController(
        program = scene.shader.program,
        config = GlSurfaceConfig(depth = true),
    )
    val linkState by controller.linkState
    Surface(color = Color(0xFF040806), modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(mode) {
                        when (mode) {
                            ClockMode.Classic -> detectTapGestures(
                                onTap = { classicSpinAt.set(System.nanoTime()) },
                            )
                            ClockMode.Ice -> detectHorizontalDragGestures { _, dragAmount ->
                                iceYaw += dragAmount * 0.002f
                            }
                            else -> Unit
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                GlSurface(
                    controller = controller,
                    mesh = scene.surfaceMesh,
                    modifier = Modifier.fillMaxSize(),
                    onFrame = { frame ->
                        val spun = classicSpinAt.get()
                        val spinElapsed = if (spun < 0L) -1f else (System.nanoTime() - spun) / 1_000_000_000f
                        frame.renderClock(scene, mode, iceYaw, spinElapsed)
                    },
                )
                if (linkState is GlLinkState.Failed) {
                    val message = (linkState as GlLinkState.Failed).message
                    GlLinkErrorOverlay(
                        message = if (message == GL_LINK_FALLBACK) {
                            stringResource(R.string.shader_link_failed)
                        } else {
                            message
                        },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            ) {
                ModeChip(
                    label = stringResource(R.string.mode_classic),
                    selected = mode == ClockMode.Classic,
                    onClick = { mode = ClockMode.Classic },
                )
                ModeChip(
                    label = stringResource(R.string.mode_dali),
                    selected = mode == ClockMode.Dali,
                    onClick = { mode = ClockMode.Dali },
                )
                ModeChip(
                    label = stringResource(R.string.mode_spheres),
                    selected = mode == ClockMode.Spheres,
                    onClick = { mode = ClockMode.Spheres },
                )
                ModeChip(
                    label = stringResource(R.string.mode_ice),
                    selected = mode == ClockMode.Ice,
                    onClick = { mode = ClockMode.Ice },
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
    )
}
