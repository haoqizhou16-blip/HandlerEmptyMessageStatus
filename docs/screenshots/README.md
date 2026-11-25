# 运行效果图

| 文件 | 状态 | 对应消息 |
| --- | --- | --- |
| `00-waiting.png` | 等待开始（初始态） | 无 |
| `01-loading.png` | 加载中，按钮变灰不可点 | `MSG_LOADING`（what = 1） |
| `02-success.png` | 任务成功，进度条消失、按钮恢复 | `MSG_SUCCESS`（what = 2） |
| `03-error.png` | 任务失败（勾选"模拟失败"后运行） | `MSG_ERROR`（what = 3） |

## 这些图是怎么来的

本机没有可用的模拟器镜像（Android SDK 缺 system-images，也没有硬件加速驱动），也无法连接真机，因此这四张图是用**本工程真实的布局文件和真实控件**，通过 Robolectric 的 native 图形模式在 JVM 里渲染导出的：加载 `activity_main.xml`、走真实的 `handleMessage()` 状态切换，再把 View 树画到 Bitmap 输出 PNG（822×1782，对应 411dp×891dp 的 xhdpi 屏幕）。图中的文字、颜色、控件都是程序真实渲染的结果，不是手绘设计稿。

两点需要说明的差异：

1. 没有系统状态栏（渲染环境没有 SystemUI），真机截图顶部会多出时间、电量一行。
2. `ProgressBar` 的不确定进度动画在静态渲染里抓不到"滚动中的蓝色段"，`01-loading.png` 中只能看到浅蓝色轨道；真机上它会来回滚动。

## 如果要在真机 / 模拟器上重拍

1. 运行 App，抓 `01-loading` 之前把 `MainActivity.FAKE_WORK_DURATION_MS` 临时改成 `8000L`，抓完改回 `2000L`；
2. 模拟器用工具栏相机图标，真机用 `adb exec-out screencap -p > 01-loading.png`；
3. 覆盖本目录同名文件即可，README 中的引用路径不用改。
