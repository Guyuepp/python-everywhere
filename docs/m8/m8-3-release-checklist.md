# M8-3 发布检查单

- 里程碑: Plan / M8-3
- 目标: 在发布前对签名、混淆、权限、隐私、质量与文档进行一次完整 Gate。
- 版本号: 1.0
- 构建类型: debug + release
- 检查日期: 2026-03-28
- 检查人: Guyuepp + Copilot

## 1. 构建与产物

- [x] `:app:assembleDebug` 通过。
- [x] `:app:assembleRelease` 通过。
- [ ] release APK/AAB 可安装并可启动。
- [x] 应用版本号与变更记录一致。

建议命令:

```bash
cd android-text-python-app
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

## 2. 签名与安装

说明: 若本次仅发布源码到 GitHub（不分发 APK/AAB），本节可标注为 `N/A`。

- [ ] release 签名配置正确。
- [ ] 目标设备安装 release 产物无冲突。
- [ ] 升级安装路径（旧版 -> 新版）验证通过。

## 3. 混淆与可观测性

说明: 若不产出 release 包，本节可标注为 `N/A`，并在发布决策中说明原因。

- [ ] 混淆策略符合发布要求（如需要）。
- [ ] 关键异常仍可定位（保留必要栈信息或错误码）。
- [ ] release 日志策略符合脱敏要求。

## 4. 权限与清单

- [ ] Manifest 权限最小化，无多余高风险权限。
- [ ] Process Text 入口声明正确。
- [ ] `launchMode` 与 `onNewIntent` 行为符合预期。

## 5. 功能闭环验证（MVP）

- [x] 启动器打开后默认进入历史记录页。
- [x] Process Text 执行完成后可在历史记录页看到新增记录。
- [x] 历史记录上限策略生效（仅保留最近 100 条）。
- [x] 清空历史操作可用并带确认步骤。

## 6. 自动化测试门禁

- [x] `:app:testDebugUnitTest` 通过。
- [x] `:app:assembleDebugAndroidTest` 通过。
- [x] `:app:connectedDebugAndroidTest` 在真机/模拟器通过。

建议命令:

```bash
cd android-text-python-app
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

## 7. 人工回归与兼容性

- [x] 浏览器类回归通过。
- [x] 阅读器类回归通过。
- [x] IM 类回归结果已记录（通过或受限）。
- [x] 兼容性限制已在用户文档中明确。

关联记录:
1. `docs/m7/m7-3-manual-regression-record.md`
2. `docs/m7/m7-4-performance-smoke.md`

## 8. 性能冒烟

- [x] 性能样本已采集并归档。
- [x] P95 指标满足门限要求。
- [x] 性能结论与风险已在文档记录。

## 9. 文档齐备性

- [x] 用户文档已更新。
- [x] 开发文档已更新。
- [x] 已知限制与替代路径已明确。
- [x] 故障排查步骤可执行。

关联文档:
1. `docs/m8/m8-1-user-guide.md`
2. `docs/m8/m8-2-developer-guide.md`

## 10. 发布决策

- 发布结论: 条件通过（带已知限制）
- 阻塞项: release 包安装与签名实机验收待执行（`installRelease` 返回成功，但设备未检测到包名且无法 `am start`）。
- 例外项: IM（微信/Telegram/QQ）不暴露系统 Process Text 菜单，属于来源应用策略限制。
- 下一步动作: 明确 release 签名与产物安装流程后重跑实机安装验证，再进入发版评审收口（debug 安装与启动链路已验证正常）。

## 11. 附录（可选）

问题定位常用命令:

```bash
adb devices
adb logcat -c
adb logcat -d | grep -E "PythonSpike|AndroidRuntime" | tail -n 200
```
