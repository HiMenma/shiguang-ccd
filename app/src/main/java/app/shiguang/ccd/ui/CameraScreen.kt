package app.shiguang.ccd.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shiguang.ccd.BuildConfig
import app.shiguang.ccd.camera.CameraSession
import app.shiguang.ccd.camera.CcdSurfaceProcessor
import app.shiguang.ccd.ui.theme.Amber
import app.shiguang.ccd.ui.theme.Cream
import app.shiguang.ccd.ui.theme.CreamDim
import app.shiguang.ccd.ui.theme.Ink
import app.shiguang.ccd.ui.theme.InkSoft
import app.shiguang.ccd.ui.theme.FilmRed
import app.shiguang.imaging.LookParams
import app.shiguang.imaging.TimestampStyle
import app.shiguang.imaging.TimestampText
import java.util.concurrent.atomic.AtomicReference

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(android.Manifest.permission.CAMERA)
    }

    if (!granted) {
        PermissionPrompt(
            onRequest = { launcher.launch(android.Manifest.permission.CAMERA) },
            onOpenSettings = {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.fromParts("package", context.packageName, null),
                )
                runCatching { context.startActivity(intent) }
            },
        )
        return
    }

    CameraBody(viewModel, state, onOpenGallery, onOpenSettings)
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Ink).systemBarsPadding().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("拾光 CCD", style = MaterialTheme.typography.displaySmall, color = Cream)
            Spacer(Modifier.height(16.dp))
            Text(
                "需要相机权限才能取景与拍摄。",
                style = MaterialTheme.typography.bodyMedium,
                color = CreamDim,
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    .background(Amber)
                    .clickable(onClick = onRequest)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("授权相机", style = MaterialTheme.typography.labelMedium, color = Ink)
            }
            Spacer(Modifier.height(10.dp))
            ToolbarButton("打开系统设置", onClick = onOpenSettings)
        }
    }
}

@Composable
private fun CameraBody(
    viewModel: CameraViewModel,
    state: CameraUiState,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val paramsRef = remember { AtomicReference(state.current) }
    SideEffect { paramsRef.set(state.current) }

    var errorText by remember { mutableStateOf<String?>(null) }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val reportError: (Throwable) -> Unit = { throwable ->
        mainHandler.post { errorText = throwable.message ?: "渲染失败" }
    }
    val processor = remember {
        CcdSurfaceProcessor(
            paramsProvider = { paramsRef.get() },
            errorListener = reportError,
        )
    }
    val session = remember {
        CameraSession(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            processor = processor,
            onError = reportError,
        )
    }

    DisposableEffect(Unit) {
        session.start()
        onDispose {
            session.stop()
            processor.release()
        }
    }
    LaunchedEffect(state.frontFacing) {
        processor.mirror = state.frontFacing
    }

    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Ink).systemBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("拾光 CCD", style = MaterialTheme.typography.labelMedium, color = Cream)
                    LcdText("v${BuildConfig.VERSION_NAME}  ·  ${state.current.name}")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ToolbarButton(flashLabel(state)) {
                        session.cycleFlash()
                        viewModel.setFlash(session.flashMode)
                    }
                    ToolbarButton("翻转") {
                        session.switchLens()
                        viewModel.setFrontFacing(session.lensFacing == CameraSelector.LENS_FACING_FRONT)
                    }
                    ToolbarButton("设置", onClick = onOpenSettings)
                }
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize(),
                    )
                    GridOverlay()
                    TimestampOverlay(state.current)
                    if (state.processing) ProcessingOverlay()
                    errorText?.let { error ->
                        Box(
                            modifier = Modifier.fillMaxSize().background(Ink.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("相机/渲染异常", style = MaterialTheme.typography.titleMedium, color = FilmRed)
                                Spacer(Modifier.height(8.dp))
                                Text(error, style = MaterialTheme.typography.bodySmall, color = CreamDim)
                            }
                        }
                    }
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.presets, key = { it.id }) { preset ->
                    PresetChip(
                        name = preset.name,
                        selected = preset.id == state.current.id,
                        onClick = {
                            viewModel.selectPreset(preset)
                            errorText = null
                        },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ToolbarButton("相册", onClick = onOpenGallery)
                ShutterButton(enabled = !state.processing) {
                    viewModel.capture(session)
                }
                ToolbarButton("参数") { viewModel.showParams(true) }
            }
        }

        if (state.showParams) {
            ParamsPanel(
                state = state,
                onDismiss = { viewModel.showParams(false) },
                onUpdate = viewModel::updateParams,
                onReset = viewModel::resetParams,
                onSavePreset = viewModel::saveAsPreset,
            )
        }
    }
}

private fun flashLabel(state: CameraUiState): String = when (state.flashMode.name) {
    "ON" -> "闪光 开"
    "AUTO" -> "闪光 自动"
    else -> "闪光 关"
}

@Composable
private fun GridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val color = Color.White.copy(alpha = 0.16f)
        val stroke = 1f
        drawLine(color, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), stroke)
        drawLine(color, Offset(size.width * 2f / 3f, 0f), Offset(size.width * 2f / 3f, size.height), stroke)
        drawLine(color, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), stroke)
        drawLine(color, Offset(0f, size.height * 2f / 3f), Offset(size.width, size.height * 2f / 3f), stroke)
    }
}

@Composable
private fun TimestampOverlay(params: LookParams) {
    if (params.timestampStyle == TimestampStyle.NONE) return
    val text = TimestampText.format(params.timestampStyle, System.currentTimeMillis())
    if (text.isEmpty()) return
    val color = if (params.timestampStyle == TimestampStyle.ORANGE_LED) Amber else Cream
    Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomEnd) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun ProcessingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Ink.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Amber, strokeWidth = 2.dp, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(10.dp))
            LcdText("冲洗中…", color = Cream)
        }
    }
}
