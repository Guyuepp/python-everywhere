# M6-1 ~ M6-4: 稳定性、安全与测试

- 里程碑: Plan v2 / M6
- 目标: 完成日志脱敏、单测补齐、仪器测试覆盖与人工回归清单。

## M6-1 日志脱敏

1. `AppLog.d` 仅在 debug 构建输出。
2. `AppLog.w` 在 release 构建不输出异常堆栈。
3. 执行路径日志仅记录长度、状态、错误码，不记录原始输入全文。

## M6-2 单元测试覆盖

1. 既有 `PythonRunnerTest` 覆盖 success/error/timeout/cancel。
2. 新增 `ProcessTextIntentReaderTest` 覆盖 intent 读取失败分支：
   - 普通读取成功。
   - 非 Process Text 默认输入。
   - 读取异常映射 `INVALID_INPUT`。
   - Binder 超限异常映射 `INTENT_TOO_LARGE`。

## M6-3 仪器测试覆盖

新增 `MainActivityProcessTextTest`：
1. Process Text 拉起后完成执行，点击复制 JSON 成功。
2. 触发 `onNewIntent` 后，新请求覆盖旧请求并渲染最新结果。

## M6-4 人工回归清单

至少覆盖三类外部应用：
1. 浏览器类：Chrome 或系统浏览器。
2. 阅读器类：文档或电子书阅读器。
3. IM 类：即时通讯应用。

每类应用执行以下步骤：
1. 选中文本并触发 Process Text 菜单项。
2. 结果页出现 Running -> Success 或 Failed，应用不崩溃。
3. 点击 Copy JSON，粘贴后 JSON 可解析且字段完整。
4. 连续触发两次，确认第二次结果覆盖第一次且无卡死。
