# M0-4: 结果协议与错误码冻结

- 里程碑: Plan v2 / M0-4
- 目标: 冻结 MVP 输出协议字段与错误码，保证 JSON 稳定可解析。

## 协议版本

- 协议名: `process_text_result`
- 协议版本: `1.0.0`（MVP 冻结）

## 冻结字段 (MVP)

下列字段在 MVP 阶段不得改名、不得删除，新增字段需向后兼容。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| protocol | string | 是 | 固定值 `process_text_result` |
| protocolVersion | string | 是 | 固定值 `1.0.0` |
| requestId | string | 是 | 请求唯一标识 |
| status | string | 是 | `success` 或 `error` 或 `cancelled` |
| errorCode | string | 否 | 失败/取消时必填 |
| message | string | 是 | 面向用户的可读摘要 |
| result | object\|null | 是 | 成功时为结构化结果，失败时为 null |
| stdout | string | 是 | Python stdout 捕获 |
| stderr | string | 是 | Python stderr 捕获 |
| traceback | string\|null | 是 | Python 异常堆栈（失败时可用） |
| startedAtMs | number | 是 | 执行开始时间戳（epoch ms） |
| finishedAtMs | number | 是 | 执行结束时间戳（epoch ms） |
| durationMs | number | 是 | 执行耗时 |
| isColdStart | boolean | 是 | 是否冷启动 Python 运行时 |
| appVersion | string | 是 | App 版本号 |
| engineVersion | string | 是 | Python 执行引擎版本 |

### 预热状态字段来源说明（`isColdStart`）

`isColdStart` 的取值来源冻结如下：

1. 在单次请求真正进入 Python 执行前，先读取 `Python.isStarted()` 快照。
2. 若快照为 `false`，则 `isColdStart=true`（本次请求触发冷启动或兜底启动）。
3. 若快照为 `true`，则 `isColdStart=false`（可能来自 Application 预热成功，或历史请求已完成启动）。
4. 预热状态（attempted/success/timeout）只作为诊断信息来源，不改变上述判定规则。

## 冻结错误码

至少包含以下错误码，并在 MVP 中冻结含义。

| 错误码 | 含义 | 触发条件 | 推荐处理 |
| --- | --- | --- | --- |
| INTENT_TOO_LARGE | Intent 负载过大 | 读取或发送阶段触发 Binder 限制 | 直接失败并提示精简输入 |
| PYTHON_INIT_TIMEOUT | Python 初始化超时 | 运行时启动超过上限 | 结束等待并返回可重试提示 |
| PYTHON_EXEC_TIMEOUT | Python 执行超时 | 执行超过上限 | 结束等待并返回超时 |
| PYTHON_CANCELLED | 用户或生命周期取消 | 用户关闭/新请求抢占/生命周期结束 | 返回取消状态，不继续等待 |

建议扩展（可选）:

| 错误码 | 含义 |
| --- | --- |
| INVALID_INPUT | 输入为空、全空白或不合法 |
| PYTHON_RUNTIME_ERROR | Python 抛出异常 |
| UNKNOWN | 未分类异常 |

## JSON 示例

### 成功

```json
{
  "protocol": "process_text_result",
  "protocolVersion": "1.0.0",
  "requestId": "8f02dd1a-62d7-4d7f-81ce-574bbf8aa9e2",
  "status": "success",
  "message": "ok",
  "result": {
    "text": "python_ok:hello"
  },
  "stdout": "",
  "stderr": "",
  "traceback": null,
  "startedAtMs": 1760000000000,
  "finishedAtMs": 1760000000120,
  "durationMs": 120,
  "isColdStart": true,
  "appVersion": "0.1.0",
  "engineVersion": "1"
}
```

### 失败

```json
{
  "protocol": "process_text_result",
  "protocolVersion": "1.0.0",
  "requestId": "8f02dd1a-62d7-4d7f-81ce-574bbf8aa9e2",
  "status": "error",
  "errorCode": "PYTHON_EXEC_TIMEOUT",
  "message": "Python execution timed out",
  "result": null,
  "stdout": "",
  "stderr": "",
  "traceback": null,
  "startedAtMs": 1760000000000,
  "finishedAtMs": 1760000005000,
  "durationMs": 5000,
  "isColdStart": false,
  "appVersion": "0.1.0",
  "engineVersion": "1"
}
```

## 完成定义 (Definition of Done)

1. 输出协议字段冻结，包含类型、必填性与含义。
2. 错误码字典冻结，至少包含 4 个强制错误码。
3. 提供成功与失败 JSON 示例，保证可解析。

## 验证命令

```bash
# 1) 检查强制错误码是否都在文档中
grep -E "INTENT_TOO_LARGE|PYTHON_INIT_TIMEOUT|PYTHON_EXEC_TIMEOUT|PYTHON_CANCELLED" docs/m0/m0-4-result-protocol.md

# 2) 检查协议关键冻结字段
grep -E "protocolVersion|requestId|status|errorCode|durationMs" docs/m0/m0-4-result-protocol.md
```
