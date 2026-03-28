# M7-3 人工回归记录（浏览器 / 阅读器 / IM）

- 里程碑: Plan / M7-3
- 目标: 至少 3 类外部 App 完成回归并留存证据。
- 测试构建: debug
- 应用版本: 1.0
- 设备型号: PLQ110 (OnePlus Ace 6)
- Android 版本: 16
- 测试日期: 2026-03-28
- 测试人: Guyuepp

## 回归前置

1. 设备连接稳定，应用已安装最新 debug 包。
2. Process Text 菜单项可见（Run as Python）。
3. 结果页支持 Copy Result JSON。

## 用例定义

### Case A: 基本执行成功

- 输入文本: `1+2*3`
- 期望:
  1. 可拉起结果页。
  2. 状态为 Success。
  3. 结果为 `7`。

### Case B: 非代码输入失败可解释

- 输入文本: `hello world`
- 期望:
  1. 可拉起结果页。
  2. 状态为 Error。
  3. message 或 traceback 可读。

### Case C: 复制 JSON 可用

- 前置: 先执行 Case A 或 Case B。
- 期望:
  1. 点击 Copy Result JSON 后可粘贴。
  2. 粘贴内容是合法 JSON。
  3. 至少包含字段: protocol/status/message/result/stdout/stderr/traceback/durationMs。

### Case D: 重复触发覆盖旧请求

- 第一次输入: `111` + `debug_delay_ms=1500`
- 第二次输入: `222` + `debug_delay_ms=0`
- 期望:
  1. 不崩溃、不卡死。
  2. 最终显示新请求结果 `222`。

## 结果记录

| App 类别 | App 名称与版本 | Case A | Case B | Case C | Case D | 结果 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 浏览器 | Chrome 146 | Pass | Pass | Pass | Pass | Pass |  |
| 阅读器 | 阅读 3.25 | Pass | Pass | Pass | Pass | Pass |  |
| IM | 微信 8.0.66 | Fail | Fail | Fail | Fail | Fail | 微信内选中文本后无法使用系统菜单 |
| IM | Telegram | Fail | Fail | Fail | Fail | Fail | 选中文本后无系统 Process Text 菜单 |
| IM | QQ | Fail | Fail | Fail | Fail | Fail | 选中文本后无系统 Process Text 菜单 |

## 证据清单

1. 每类 App 至少 1 张结果页截图。
2. 至少 1 份成功 JSON 和 1 份失败 JSON 粘贴文本。
3. 若失败，附 logcat 关键信息（AndroidRuntime/PythonSpike）。

## 结论

- 执行结果汇总: 浏览器 Pass、阅读器 Pass、IM Fail（微信/Telegram/QQ 均失败）。
- 通过率: 2/3 类应用通过。
- M7-3 验收结论: 暂不通过（未满足“三类外部 App 回归通过”）。
- 失败原因: 当前已测试的 IM 应用（微信/Telegram/QQ）均不暴露可用系统 Process Text 菜单，A/B/C/D 无法在 IM 内完成。
- 建议动作:
  1. 在用户文档中标注“部分 IM 不支持系统 Process Text 菜单”为已知限制。
  2. 提供替代路径：将文本复制到支持 Process Text 的应用（浏览器/阅读器/系统编辑器）后再执行。
  3. 若后续发现支持该菜单的 IM，再补测并更新 M7-3 记录。

