package app.shiguang.ccd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shiguang.ccd.BuildConfig
import app.shiguang.ccd.ui.theme.Amber
import app.shiguang.ccd.ui.theme.Cream
import app.shiguang.ccd.ui.theme.CreamDim
import app.shiguang.ccd.ui.theme.Ink

@Composable
fun SettingsScreen(viewModel: CameraViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().background(Ink).systemBarsPadding().padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ToolbarButton("返回", onClick = onBack)
            Text("设置", style = MaterialTheme.typography.labelMedium, color = Cream)
            Text(" ", style = MaterialTheme.typography.labelSmall)
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("保留原图", style = MaterialTheme.typography.titleMedium, color = Cream)
                Text(
                    "关闭后不保留原始 JPEG，之后只能对成片再处理",
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamDim,
                )
            }
            Switch(
                checked = state.keepOriginal,
                onCheckedChange = viewModel::setKeepOriginal,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Ink,
                    checkedTrackColor = Amber,
                ),
            )
        }

        Spacer(Modifier.height(32.dp))
        Text("关于", style = MaterialTheme.typography.labelMedium, color = Cream)
        Spacer(Modifier.height(8.dp))
        Text(
            "拾光 CCD ${BuildConfig.VERSION_NAME}\n" +
                "自研 CCD 风格相机：实时 GPU 滤镜、6 个内置预设、10 项参数微调、相册导入编辑。\n\n" +
                "本应用不申请网络权限，不收集任何数据。所有照片只保存在本机相册的「拾光CCD」目录。",
            style = MaterialTheme.typography.bodySmall,
            color = CreamDim,
        )
    }
}
