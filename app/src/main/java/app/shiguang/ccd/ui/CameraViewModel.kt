package app.shiguang.ccd.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.shiguang.ccd.ShiguangApp
import app.shiguang.ccd.camera.CameraSession
import app.shiguang.ccd.camera.FlashSetting
import app.shiguang.ccd.data.PhotoWorkbench
import app.shiguang.ccd.data.decodeCapturedFile
import app.shiguang.imaging.LookParams
import app.shiguang.imaging.PresetLibrary
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CameraUiState(
    val presets: List<LookParams> = PresetLibrary.builtIn,
    val current: LookParams = PresetLibrary.default,
    val processing: Boolean = false,
    val message: String? = null,
    val lastPhoto: Uri? = null,
    val flashMode: FlashSetting = FlashSetting.OFF,
    val frontFacing: Boolean = false,
    val showParams: Boolean = false,
    val keepOriginal: Boolean = false,
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as ShiguangApp).container
    val workbench = PhotoWorkbench(container)

    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val custom = container.settings.customPresets.first()
            val lastId = container.settings.lastPresetId.first()
            val keep = container.settings.keepOriginal.first()
            val presets = PresetLibrary.catalog(custom)
            val current = presets.firstOrNull { it.id == lastId } ?: PresetLibrary.default
            _state.update { it.copy(presets = presets, current = current, keepOriginal = keep) }
        }
    }

    fun selectPreset(preset: LookParams) {
        _state.update { it.copy(current = preset) }
        if (PresetLibrary.builtIn.any { it.id == preset.id }) {
            viewModelScope.launch { container.settings.setLastPreset(preset.id) }
        }
    }

    fun updateParams(transform: (LookParams) -> LookParams) {
        _state.update { it.copy(current = transform(it.current).sanitized()) }
    }

    fun resetParams() {
        val id = _state.value.current.id
        val base = PresetLibrary.byId(id)
        if (base != null) {
            _state.update { it.copy(current = base) }
        }
    }

    fun setFlash(mode: FlashSetting) {
        _state.update { it.copy(flashMode = mode) }
    }

    fun setFrontFacing(front: Boolean) {
        _state.update { it.copy(frontFacing = front) }
    }

    fun showParams(show: Boolean) {
        _state.update { it.copy(showParams = show) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun setKeepOriginal(value: Boolean) {
        _state.update { it.copy(keepOriginal = value) }
        viewModelScope.launch { container.settings.setKeepOriginal(value) }
    }

    fun saveAsPreset() {
        viewModelScope.launch {
            val current = _state.value.current
            val custom = container.settings.customPresets.first().filterNot { it.id == current.id }
            val copy = current.copy(
                id = "custom_${System.currentTimeMillis()}",
                name = if (current.name.endsWith("·存")) current.name else "${current.name}·存",
            )
            val updated = custom + copy
            container.settings.setCustomPresets(updated)
            _state.update {
                it.copy(presets = PresetLibrary.catalog(updated), message = "已保存为新预设")
            }
        }
    }

    fun capture(session: CameraSession) {
        if (_state.value.processing) return
        val snapshot = _state.value
        val params = snapshot.current
        // The camera HAL already delivers front-camera JPEGs in the same
        // orientation the user sees in the (mirrored) preview. Flipping here
        // again would make the saved photo disagree with the viewfinder.
        val mirror = false
        _state.update { it.copy(processing = true) }
        val app = getApplication<Application>()
        val key = "SG_${System.currentTimeMillis()}"
        val file = File(app.cacheDir, "cap_$key.jpg")
        session.captureTo(
            file = file,
            onSaved = {
                viewModelScope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.IO) { container.originals.save(key, file) }
                        val source = withContext(Dispatchers.IO) { decodeCapturedFile(file) }
                        val rendered = workbench.render(source, params, mirror)
                        workbench.finishAndSave(rendered, params, name = "$key.jpg")
                    }
                    file.delete()
                    if (result.isSuccess && !snapshot.keepOriginal) {
                        withContext(Dispatchers.IO) { container.originals.delete(key) }
                    }
                    _state.update {
                        it.copy(
                            processing = false,
                            lastPhoto = result.getOrNull() ?: it.lastPhoto,
                            message = result.exceptionOrNull()?.let { e -> "保存失败：${e.message}" },
                        )
                    }
                }
            },
            onFailure = { throwable ->
                file.delete()
                _state.update { it.copy(processing = false, message = "拍摄失败：${throwable.message}") }
            },
        )
    }

    suspend fun recentPhotos(limit: Int = 60) = container.gallery.recent(limit)

    suspend fun thumbnail(uri: Uri, size: Int = 480): Bitmap = container.gallery.load(uri, size)

    suspend fun originalPreview(uri: Uri, maxSize: Int = 1600): Bitmap = editorSource(uri, maxSize)

    suspend fun renderEdit(uri: Uri, maxSize: Int = 1600): Bitmap {
        val source = editorSource(uri, maxSize)
        return workbench.render(source, _state.value.current)
    }

    suspend fun saveEdited(uri: Uri): Result<Uri> = runCatching {
        val keep = _state.value.keepOriginal
        val source = editorSource(uri, 4096)
        val rendered = workbench.render(source, _state.value.current)
        val saved = workbench.finishAndSave(rendered, _state.value.current)
        if (!keep) {
            val key = withContext(Dispatchers.IO) { displayName(uri) }?.substringBeforeLast('.')
            if (key != null) withContext(Dispatchers.IO) { container.originals.delete(key) }
        }
        saved
    }

    /**
     * Prefer the sidecar original captured with the shot: re-editing a finished
     * photo would otherwise stack the look a second time.
     */
    private suspend fun editorSource(uri: Uri, maxSize: Int): Bitmap {
        val name = withContext(Dispatchers.IO) { displayName(uri) }
        val key = name?.substringBeforeLast('.')
        val sidecar = key?.let { container.originals.file(it) }
        return if (sidecar != null) {
            withContext(Dispatchers.IO) { decodeCapturedFile(sidecar, maxSize) }
        } else {
            container.gallery.load(uri, maxSize)
        }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        container.gallery.displayName(uri)
    }.getOrNull()
}
