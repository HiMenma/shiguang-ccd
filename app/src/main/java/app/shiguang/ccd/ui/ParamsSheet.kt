package app.shiguang.ccd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.shiguang.ccd.ui.theme.Amber
import app.shiguang.ccd.ui.theme.Cream
import app.shiguang.ccd.ui.theme.CreamDim
import app.shiguang.ccd.ui.theme.InkSoft
import app.shiguang.imaging.LookParams
import java.util.Locale

/**
 * In-layout parameter panel.  A plain overlay rather than a dialog window:
 * cheaper to compose and it keeps the live preview visible behind it.
 */
@Composable
fun ParamsPanel(
    state: CameraUiState,
    onDismiss: () -> Unit,
    onUpdate: ((LookParams) -> LookParams) -> Unit,
    onReset: () -> Unit,
    onSavePreset: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.86f),
            color = InkSoft,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp, bottom = 18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("参数", style = MaterialTheme.typography.labelMedium, color = Cream)
                        Text(
                            state.current.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = CreamDim,
                        )
                    }
                    ToolbarButton("收起", onClick = onDismiss)
                }
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    ParamSlider("曝光", state.current.exposure, -1.5f..1.5f) { v ->
                        onUpdate { it.copy(exposure = v) }
                    }
                    ParamSlider("色温", state.current.kelvin, 3000f..9000f) { v ->
                        onUpdate { it.copy(kelvin = v) }
                    }
                    ParamSlider("色调", state.current.tint, -0.5f..0.5f) { v ->
                        onUpdate { it.copy(tint = v) }
                    }
                    ParamSlider("饱和", state.current.saturation, 0f..1.8f) { v ->
                        onUpdate { it.copy(saturation = v) }
                    }
                    ParamSlider("中间调", state.current.midGamma, 0.7f..1.4f) { v ->
                        onUpdate { it.copy(midGamma = v) }
                    }
                    ParamSlider("颗粒", state.current.grainAmount, 0f..1f) { v ->
                        onUpdate { it.copy(grainAmount = v) }
                    }
                    ParamSlider("暗角", state.current.vignette, 0f..0.6f) { v ->
                        onUpdate { it.copy(vignette = v) }
                    }
                    ParamSlider("色散", state.current.chromaticAberration, 0f..1f) { v ->
                        onUpdate { it.copy(chromaticAberration = v) }
                    }
                    ParamSlider("柔光", state.current.bloomStrength, 0f..0.5f) { v ->
                        onUpdate { it.copy(bloomStrength = v) }
                    }
                    ParamSlider("清晰度", state.current.sharpen, 0f..1f) { v ->
                        onUpdate { it.copy(sharpen = v) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToolbarButton("重置", onClick = onReset)
                        ToolbarButton("存为预设", onClick = onSavePreset)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "参数即时作用于取景画面，并随照片一起固化。",
                        style = MaterialTheme.typography.labelSmall,
                        color = CreamDim,
                    )
                }
            }
        }
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Cream)
            Text(
                formatValue(label, value),
                style = MaterialTheme.typography.labelSmall,
                color = CreamDim,
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Amber,
                activeTrackColor = Amber,
                inactiveTrackColor = CreamDim.copy(alpha = 0.25f),
            ),
        )
    }
}

private fun formatValue(label: String, value: Float): String = when (label) {
    "色温" -> "${value.toInt()}K"
    "曝光" -> String.format(Locale.US, "%+.2f", value)
    else -> String.format(Locale.US, "%.2f", value)
}
