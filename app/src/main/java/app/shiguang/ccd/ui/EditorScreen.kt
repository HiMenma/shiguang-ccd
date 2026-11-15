package app.shiguang.ccd.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shiguang.ccd.ui.theme.Amber
import app.shiguang.ccd.ui.theme.Cream
import app.shiguang.ccd.ui.theme.CreamDim
import app.shiguang.ccd.ui.theme.Ink
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    viewModel: CameraViewModel,
    uri: Uri,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showOriginal by remember { mutableStateOf(false) }

    val rendered by produceState<Bitmap?>(initialValue = null, uri, state.current) {
        value = runCatching { viewModel.renderEdit(uri) }.getOrNull()
    }
    val original by produceState<Bitmap?>(initialValue = null, uri) {
        value = runCatching { viewModel.originalPreview(uri) }.getOrNull()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Ink).systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ToolbarButton("返回", onClick = onBack)
            Text(
                if (showOriginal) "原图" else "效果 · ${state.current.name}",
                style = MaterialTheme.typography.labelMedium,
                color = Cream,
            )
            ToolbarButton("保存") {
                scope.launch {
                    val result = viewModel.saveEdited(uri)
                    Toast.makeText(
                        context,
                        result.fold({ "已保存到相册" }, { "保存失败：${it.message}" }),
                        Toast.LENGTH_SHORT,
                    ).show()
                    if (result.isSuccess) onBack()
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = if (showOriginal) original else rendered
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                CircularProgressIndicator(color = Amber)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                showOriginal = true
                                tryAwaitRelease()
                                showOriginal = false
                            },
                        )
                    },
            )
        }

        Text(
            "按住画面查看原图",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = CreamDim,
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.presets, key = { it.id }) { preset ->
                PresetChip(
                    name = preset.name,
                    selected = preset.id == state.current.id,
                    onClick = { viewModel.selectPreset(preset) },
                )
            }
        }
    }
}
