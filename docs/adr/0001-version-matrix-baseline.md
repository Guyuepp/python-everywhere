# ADR-0001: Android Process Text Python 版本矩阵基线（M0-1）

- 状态: Accepted
- 日期: 2026-03-28
- 关联计划: Plan v2 / M0-1

## 背景

M0-1 要求冻结 Android + Python 构建链版本，避免 AGP/Kotlin/Chaquopy 兼容性漂移导致后续里程碑不可复现。

## 决策

采用以下固定版本组合，禁止使用 `latest` 或浮动版本：

| 组件 | 锁定版本 | 依据 |
| --- | --- | --- |
| Android Gradle Plugin | `9.1.0` | Android 官方 release notes（2026-03） |
| Gradle Wrapper | `9.3.1` | AGP 9.1.0 兼容性表要求 |
| Kotlin Gradle Plugin | `2.3.20` | Kotlin 官方稳定版（2026-03）；AGP 9 内置 Kotlin，使用 `buildscript classpath` 固定为 2.3.20 |
| Chaquopy Plugin | `17.0.0` | Chaquopy 当前文档给出的插件版本 |
| Python (Chaquopy runtime) | `3.14` | Chaquopy 文档支持版本（3.10~3.14）；M0 Spike 选择最新可用稳定项 |

## 约束与边界

1. `minSdk` 固定为 `26`（满足计划要求，且高于 Chaquopy 最低 `24`）。
2. 不启用动态版本号、版本范围、`+` 号依赖。
3. 仅允许在后续 ADR 更新后升级版本矩阵。
4. AGP 9 项目不应用 `org.jetbrains.kotlin.android` 插件，避免与 built-in Kotlin 冲突。

## 影响

1. 构建与 CI 可复现性提升，减少“本地可跑、CI 失败”风险。
2. 后续若升级 AGP 或 Kotlin，必须同时回归验证 Chaquopy 兼容。

## 验证记录

M0-2 Spike 使用上述版本组合创建，执行 Gradle 任务用于验证构建链可解析。

## 参考

1. Chaquopy Gradle plugin 文档（current）: 插件版本 `17.0.0`，AGP 支持区间 `7.3.x`~`9.1.x`，Python 版本可选 `3.10`~`3.14`。
2. Android Gradle Plugin 9.1.0 release notes: 兼容 Gradle `9.3.1`、JDK `17`。
3. AGP 9.0 release notes (Built-in Kotlin): 默认内置 Kotlin，最低 KGP `2.2.10`，可通过 `buildscript` 升级到更高版本。
4. Android Kotlin support: Kotlin `2.3` 需要 AGP >= `8.13.2`。
5. Kotlin releases: 最新稳定版 `2.3.20`（2026-03-16）。
