# Android Process Text + 本地 Python（Chaquopy）

English version: [README.md](README.md)

这是一个 Android 应用：通过系统 Process Text 菜单，把你选中的文本当作 Python 代码在本地执行，并以结构化结果展示。

## 功能简介

- 使用 Android Process Text 作为系统入口。
- 基于 Chaquopy 在本地离线执行 Python 代码。
- 展示执行状态：Running / Success / Error。
- 支持展开查看：`stdout`、`stderr`、`traceback`。
- 支持一键复制标准化结果 JSON。

## 致谢

特别感谢 GitHub Copilot（GPT-5.3-Codex）在开发阶段贡献了大部分实现代码、测试脚手架与文档整理工作。

## 项目结构

- `android-text-python-app/`：Android 应用工程。
- `docs/`：设计、里程碑验收、测试与发布文档。
  - `docs/m8/m8-1-user-guide.md`：用户手册。
  - `docs/m8/m8-2-developer-guide.md`：开发手册。
  - `docs/m8/m8-3-release-checklist.md`：发布检查单。
  - `docs/m8/m8-4-mvp-acceptance-review.md`：MVP 验收评审。

## 发布版本

- Release 页面：https://github.com/Guyuepp/python-everywhere/releases
- 变更记录：[CHANGELOG.md](CHANGELOG.md)

## 技术栈

- Android（Kotlin + View）
- Chaquopy（内嵌 Python）
- Coroutines（执行与取消控制）
- JUnit + AndroidX Test / Espresso

版本矩阵见：
- `docs/adr/0001-version-matrix-baseline.md`

## 构建与测试

在 `android-text-python-app/` 目录执行：

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

## 安装

```bash
cd android-text-python-app
./gradlew :app:installDebug
```

## 基本使用

1. 在支持 Process Text 的应用中选中文本。
2. 点击 `Run as Python`。
3. 查看执行状态与输出详情。
4. 需要时点击 `Copy Result JSON`。

## 已知限制

部分应用不会暴露 Android 系统 Process Text 菜单。实测中，部分 IM 应用（微信 / Telegram / QQ）存在该限制。

建议绕行方案：
- 先把文本复制到支持 Process Text 的应用（浏览器/阅读器/编辑器）中，再执行。

## 许可证

本项目使用 MIT License，详情见 `LICENSE`。
