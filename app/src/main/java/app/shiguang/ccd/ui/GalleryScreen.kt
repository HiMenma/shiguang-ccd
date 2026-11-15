package app.shiguang.ccd.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.shiguang.ccd.data.GalleryItem
import app.shiguang.ccd.ui.theme.Cream
import app.shiguang.ccd.ui.theme.CreamDim
import app.shiguang.ccd.ui.theme.Ink
import app.shiguang.ccd.ui.theme.InkSoft

@Composable
fun GalleryScreen(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    onOpen: (Uri) -> Unit,
) {
    val items by produceState<List<GalleryItem>>(initialValue = emptyList()) {
        value = runCatching { viewModel.recentPhotos() }.getOrDefault(emptyList())
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
                "相册 · ${items.size}",
                style = MaterialTheme.typography.labelMedium,
                color = Cream,
            )
            Text(" ", style = MaterialTheme.typography.labelSmall)
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "还没有照片，去拍一张吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CreamDim,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                items(items, key = { it.uri.toString() }) { item ->
                    GalleryCell(viewModel, item.uri, onOpen)
                }
            }
        }
    }
}

@Composable
private fun GalleryCell(viewModel: CameraViewModel, uri: Uri, onOpen: (Uri) -> Unit) {
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = runCatching { viewModel.thumbnail(uri) }.getOrNull()
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(InkSoft)
            .clickable { onOpen(uri) },
    ) {
        bitmap?.let { image ->
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
