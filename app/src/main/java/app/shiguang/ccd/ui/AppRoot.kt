package app.shiguang.ccd.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

sealed interface Screen {
    data object Camera : Screen
    data object Gallery : Screen
    data class Editor(val uri: Uri) : Screen
    data object Settings : Screen
}

@Composable
fun AppRoot(viewModel: CameraViewModel = viewModel()) {
    var screen by remember { mutableStateOf<Screen>(Screen.Camera) }

    when (val current = screen) {
        Screen.Camera -> CameraScreen(
            viewModel = viewModel,
            onOpenGallery = { screen = Screen.Gallery },
            onOpenSettings = { screen = Screen.Settings },
        )

        Screen.Gallery -> GalleryScreen(
            viewModel = viewModel,
            onBack = { screen = Screen.Camera },
            onOpen = { screen = Screen.Editor(it) },
        )

        is Screen.Editor -> EditorScreen(
            viewModel = viewModel,
            uri = current.uri,
            onBack = { screen = Screen.Gallery },
        )

        Screen.Settings -> SettingsScreen(
            viewModel = viewModel,
            onBack = { screen = Screen.Camera },
        )
    }
}
