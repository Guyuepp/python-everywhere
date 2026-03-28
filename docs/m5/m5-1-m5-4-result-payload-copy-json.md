# M5-1 ~ M5-4: ResultPayload 与一键复制 JSON

- 里程碑: Plan v2 / M5-1, M5-2, M5-3, M5-4
- 目标: 将执行结果映射为稳定 JSON 协议，并在结果页提供一键复制与反馈。

## 关键实现

1. ResultPayload 中间模型：
   - 新增 `ResultPayload`，固定字段：
     - `protocol`, `protocolVersion`, `requestId`, `status`, `errorCode`, `message`
     - `result`, `stdout`, `stderr`, `traceback`, `isColdStart`, `durationMs`
   - 通过 `ResultPayload.fromExecution(...)` 统一映射 `PythonResultPayload`。

2. 稳定 JSON 序列化：
   - `toJsonString()` 使用 `JSONObject` 序列化。
   - 序列化异常时回退为合法错误 JSON，保证调用方始终拿到可解析字符串。

3. 结果页复制能力：
   - 结果对话框头部新增 `Copy JSON` 按钮。
   - 执行中禁用复制；结果渲染后启用。
   - 点击按钮会把当前结果 JSON 复制到系统剪贴板。

4. 反馈与容错：
   - 无可用 JSON 时提示 `No JSON available yet`。
   - 复制成功提示 `JSON copied`。
   - 复制失败提示 `Copy failed`，并记录日志便于定位。

5. 单测覆盖：
   - 新增 `ResultPayloadTest`，覆盖 success/error/cancelled/timeout 序列化场景。
   - 校验关键字段稳定性与 `durationMs`、`isColdStart` 一致性。

## 验证命令

```bash
cd android-text-python-app
./gradlew :app:assembleDebug
./gradlew :app:lint
./gradlew :app:testDebugUnitTest
```

## 手动验证（复制 JSON）

1. 通过 Process Text 触发一次执行，等待结果页进入成功或失败状态。
2. 点击 `Copy JSON`。
3. 在任意可粘贴输入框中粘贴，确认是合法 JSON 且字段完整。
4. 在执行中 `Copy JSON` 按钮应为禁用状态；待结果渲染完成后按钮可点击。
