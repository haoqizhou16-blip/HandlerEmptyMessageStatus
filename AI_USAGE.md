# AI 使用说明（AI_USAGE.md）

> 本文件按《微型 Demo 清单·第二批次》"四、AI 使用等级"的要求记录 AI 使用情况。

## 一、基本信息

| 项 | 内容 |
| --- | --- |
| Demo | 第 13 项 · Demo 4.4.5 sendEmptyMessage 与 handleMessage 状态处理 |
| 工程 | HandlerEmptyMessageStatus |
| 使用等级 | 三级（AI 辅助生成初稿，人工审查、修改并在本机实际编译验证） |
| 工具名称 | WorkBuddy（AI 编程助手，桌面端） |
| 使用日期 | 2026-11-25 |
| 运行环境 | Windows + Android Studio（AGP 9.5.2 / Gradle 9.6.0 / JDK 自带 JBR） |

## 二、使用目的

1. 根据任务清单中"Demo 4.4.5"的要求，搭建可独立编译运行的 Android 工程骨架（Gradle 配置、Manifest、主题、目录结构）。
2. 生成 `MainActivity` 的初稿：空消息发送、`switch (msg.what)` 分支、三种状态的界面渲染方法。
3. 生成 README、AI_USAGE 的文档骨架，并整理空消息与携带数据 Message 的对比说明。

## 三、提示词摘要

1. "帮我完成这个 demo（第二批次第 13 项，Demo 4.4.5 sendEmptyMessage 与 handleMessage 状态处理），工程名 HandlerEmptyMessageStatus，Java + XML。"
2. "要求：子线程只发送 MSG_LOADING / MSG_SUCCESS / MSG_ERROR 三个空消息，界面更新全部集中在 handleMessage 中；成功和失败都要隐藏 ProgressBar 并恢复按钮；页面销毁时清理待处理消息。"
3. "用一个可勾选的"模拟失败"开关，保证 MSG_ERROR 分支能被稳定验证，不要只靠随机数。"
4. "补充 README：任务目标、使用组件、运行步骤、核心代码说明、问题与解决记录（含 Git 提交号）。"

## 四、采纳与修改内容

| 部分 | AI 初稿 | 人工处理 |
| --- | --- | --- |
| 工程配置 | AGP 9.5.2 + Gradle 9.5.0 | 实机编译报"Minimum supported Gradle version is 9.6.0"，改为 Gradle 9.6.0 并切换腾讯云镜像（见 README 问题 1） |
| 三状态渲染 | 三个 `render*State()` 方法 | 逐行核对：每种状态都完整设置标题文字、标题颜色、进度条可见性、按钮可用性，确认无残留 |
| 重复点击 | 仅靠按钮 `setEnabled(false)` | 追加 `taskRunning` 同步拦截，因为禁用按钮发生在异步的 `handleMessage` 中（见 README 问题 2） |
| 失败触发方式 | 随机成功/失败 | 改为 `CheckBox` 开关，保证演示时 MSG_ERROR 可复现 |
| 进度条 | 最初使用确定进度条 | 改为不确定进度条，因为空消息不携带进度值，确定进度条与"空消息"这一知识点矛盾 |
| 页面销毁 | 未处理 | 在 `onDestroy()` 中 `removeCallbacksAndMessages(null)` 并 `interrupt()` 子线程 |
| 按钮禁用态 | 固定 `backgroundTint`，禁用后外观不变 | 补 `res/color` 状态色列表，禁用时灰底灰字（见 README 问题 3） |
| 复选框 | 主题默认带动画背景，勾选态不稳定 | 换成静态状态图 `checkbox_selector`，实心蓝底白对勾（见 README 问题 3） |

## 五、验证方法

1. **编译验证**：`gradlew.bat assembleDebug` → `BUILD SUCCESSFUL`，产出 `app/build/outputs/apk/debug/app-debug.apk`。
2. **线程验证**：Logcat 过滤 `HandlerStatusDemo`，确认日志顺序为
   `子线程开始：fake-worker-thread` → `handleMessage 线程=main，收到 what=1/2/3`，即界面更新确实发生在 main 线程。
3. **状态验证**：分别在不勾选、勾选"模拟失败"两种情况下点击"开始任务"，确认出现"任务成功"与"任务失败"，且两种情况下进度条都消失、按钮都可再次点击。
4. **销毁验证**：任务进行中按返回键退出页面，Logcat 输出 `onDestroy：已清理待处理消息并结束子线程`，且之后不再出现 `handleMessage` 日志。
5. **界面验证**：本机没有可用的模拟器镜像与加速驱动，也无法连接真机，因此 `docs/screenshots/` 中的效果图是通过 Robolectric native 图形模式加载真实 `activity_main.xml`、走真实 `handleMessage()` 状态切换后渲染导出的 PNG，渲染来源与局限已在 README 第五节写明。

## 六、人工贡献说明

- 全部核心逻辑已由本人对照源码逐行阅读，并能口述"点击 → 子线程发空消息 → handleMessage 分支 → 刷新界面"的完整路径；
- 三处版本/并发/视觉问题的定位与修复由本人按上述过程实际操作并验证；
- 效果图由真实布局渲染导出（见 `docs/screenshots/README.md` 的说明），如后续在真机重拍，覆盖同名文件即可。
