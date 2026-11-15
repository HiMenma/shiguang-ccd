# 拾光 CCD · 设计说明

## 1. 渲染管线

所有画面（预览与成片）走同一条管线，参数来自 `LookParams`：

```
相机帧 (OES 纹理)
  │
  ├─ ① scene pass：白平衡增益 → 3×3 色彩矩阵 → 色调曲线 → 分离色调 → 饱和度
  │      输出到 renderScale 比例的 FBO（0.55–1.0，用来模拟低分辨率）
  │
  ├─ ② bright pass：按亮度阈值提取高光，缩到 1/4 分辨率
  │
  ├─ ③ blur ×2：9-tap 高斯，横向 + 纵向（ping-pong）
  │
  └─ ④ composite：泛光叠加 → 径向色散 → 反锐化 → 颗粒 → 暗角 → 抖动
         输出到屏幕（预览）或离屏 FBO（拍摄/导入）
```

工作空间是 sRGB、8-bit，不做线性化——这是刻意模仿 2000 年代机内 JPEG 的处理方式：
高光容易过曝、暗部容易死黑、锐化与饱和度都在 gamma 空间做。

### 1.1 色调曲线

```
y = x · 2^exposure
if y > 1 - shoulder:  y = (1 - shoulder) + shoulder · tanh((y - (1 - shoulder)) / shoulder)
y = y ^ midGamma
y = y ^ (1 / (1 + toe))
y = blackLift + y · (1 - blackLift)
y = y · white
```

肩部用 tanh，在膝点处一阶连续（导数 = 1），不会产生亮部断层；`toe` 抬暗部、`blackLift` 抬黑位，
两者一起制造「老数码」的灰雾感。Kotlin 版 `imaging/ToneCurve.kt` 与 GLSL 版逐行对应。

### 1.2 白平衡

`kelvinToRgb()` 用 Tanner Helland 近似，以 5500K 为参考算出 R/G/B 增益，
再按 Rec.709 亮度归一化——拖动色温不会顺带改变曝光。`tint` 只压绿色通道（正值偏品红）。

### 1.3 颗粒

`hash(gl_FragCoord.xy * grainSize + seed)` 的哈希噪声，亮度越高颗粒越弱（`0.18` 系数），
每帧 seed 变化 → 预览有「活」的颗粒感；成片用固定 seed，保证可复现。

### 1.4 色散 / 暗角 / 降采样

- 色散：R/B 通道沿径向偏移 `±dir · ca · 0.004`，中心无偏移、边缘最强。
- 暗角：`1 - vignette · smoothstep(0.35, 0.95, r·√2)`。
- 降采样：scene FBO 按 `renderScale` 缩小后由 composite 双线性放大，制造低像素的软边。

## 2. 参数表

| 字段 | 范围 | 作用 |
|---|---|---|
| `kelvin` | 2500–10000 | 色温，5500 中性 |
| `tint` | -1–1 | 绿/品红偏移 |
| `matrix` | 3×3 | 通道混合（预设用） |
| `exposure` | -2–2 EV | 曝光 |
| `blackLift` | 0–0.3 | 抬黑位（褪色感） |
| `toe` | 0–0.9 | 抬暗部 |
| `midGamma` | 0.5–2 | 中间调 |
| `shoulder` | 0–0.6 | 高光压缩 |
| `white` | 0.7–1 | 白点 |
| `saturation` | 0–2 | 饱和度 |
| `shadowTint` / `highlightTint` | ±0.25 | 分离色调 |
| `bloomStrength` | 0–0.6 | 泛光 |
| `chromaticAberration` | 0–1 | 色散 |
| `vignette` | 0–0.8 | 暗角 |
| `grainAmount` / `grainSize` | 0–1 / 0.5–3 | 颗粒 |
| `sharpen` | 0–1 | 反锐化 |
| `renderScale` | 0.4–1 | 降采样比例 |
| `timestampStyle` | NONE / ORANGE_LED / WHITE_DIGITAL | 日期戳 |
| `borderStyle` | NONE / PAPER_WHITE | 撕拉白边 |

## 3. 预设

| 预设 | 色温 | 饱和 | 颗粒 | 降采样 | 特征 |
|---|---|---|---|---|---|
| 奶油 | 5850K | 1.00 | 0.20 | 1.00 | 抬黑位、粉调肤色、柔光 |
| 千禧 | 5600K | 1.15 | 0.50 | 0.55 | 低像素 + 紫调泛光 + 橙日期戳 |
| 街拍 | 5400K | 1.20 | 0.32 | 0.85 | 高对比、青绿暗部、锐 |
| 蓝调 | 4700K | 0.85 | 0.28 | 0.90 | 冷白平衡、蓝绿暗部 |
| 银盐 | 5500K | 0.00 | 0.65 | 0.90 | 通道混合黑白、深黑 |
| 撕拉 | 6500K | 0.95 | 0.40 | 0.80 | 暖黄、白边、白日期戳 |

## 4. 线程模型

- **预览**：`CcdSurfaceProcessor` 持有独立 `HandlerThread("ccd-gl")`，CameraX 的回调全部 post 到该线程；
  `SurfaceTexture` 每来一帧就 `updateTexImage()` → 渲染 → `eglSwapBuffers`。
- **拍摄**：`ImageCapture` 写缓存 JPEG → 后台线程解码/EXIF 纠正 → `LookRenderer.renderBitmap()`（独立 EGL 上下文）→ 叠加日期戳/边框 → MediaStore。
- **导入**：PhotoPicker/相册 URI → 按 `maxSize` 降采样 → 同一套离屏渲染。

## 5. 原图与重编辑

每次拍摄除了输出成片，还会把相机原始 JPEG 存到应用私有目录 `files/originals/<key>.jpg`。
编辑页优先读这个原图再套滤镜，避免对已经处理过的成片二次叠加；设置里的「保留原图」关闭时，
成片保存后会删除 sidecar。

## 6. 已知偏差

- 预览与成片的颗粒 seed 不同，静态截图不会完全一致。
- 前摄镜像在**纹理空间**（矩阵之前）应用。若放在矩阵之后，会与前置相机 180° 的变换叠加成上下翻转，
  这正是 0.1.0 修复的「前置取景倒立」问题；成片不再二次翻转，保持与取景一致。
- 不同厂商 HAL 可能已对前置 JPEG 做一次镜像，成片方向以 HAL 输出为准。
- 模拟器（SwiftShader）性能低，预览帧率与处理耗时只用于验证链路，不代表真机表现。
- 预览是 3:4，与 ImageCapture 的输出比例一致；若厂商 HAL 给出别的比例，PreviewView 会裁切。
