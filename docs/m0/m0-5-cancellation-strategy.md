# M0-5: 取消策略冻结（协作取消 + 放弃等待）

- 里程碑: Plan v2 / M0-5
- 目标: 冻结取消语义与实现路线，禁止线程强杀。

## 核心结论

1. Kotlin 侧使用 cancel token 标记取消意图。
2. Python 侧轮询 token，检测到取消后主动退出。
3. 超时后采用“放弃等待 + 状态回传”，不做线程强杀。
4. 明确禁止 `Thread.stop`、禁止通过中断强行终止 Python 执行线程。

## 执行模型

1. Kotlin 发起请求时创建 `requestId` 与 `cancelToken`。
2. 在 `Dispatchers.IO` 执行 Python 调用。
3. Kotlin 将 token 传入 Python 入口参数（或共享状态查询函数）。
4. Python 在可中断点轮询 token:
   - 未取消: 继续执行
   - 已取消: 立即返回 `PYTHON_CANCELLED`
5. 达到超时时间后:
   - Kotlin 不再等待本次结果（放弃等待）
   - 立即回传 `PYTHON_EXEC_TIMEOUT`
   - 并发闸门确保后续请求不会与脏状态并发冲突

## Python 轮询建议

1. 在循环、批处理、分段 I/O 前后检查 token。
2. 计算密集型任务按步长检查（例如每 N 次迭代一次）。
3. 检查频率目标: 正常负载下取消响应时间 <= 200ms（目标值，可按设备调整）。

## 状态与错误码映射

| 场景 | status | errorCode |
| --- | --- | --- |
| 用户主动关闭页面 | cancelled | PYTHON_CANCELLED |
| 生命周期导致取消 | cancelled | PYTHON_CANCELLED |
| 达到执行超时阈值 | error | PYTHON_EXEC_TIMEOUT |
| Python 初始化阶段超时 | error | PYTHON_INIT_TIMEOUT |

## 禁止项

1. 禁止 `Thread.stop`。
2. 禁止依赖线程中断来强制终止 Python 解释器。
3. 禁止在超时后继续阻塞 UI 等待 Python 结束。

## 完成定义 (Definition of Done)

1. 取消策略文档明确 Kotlin token、Python 轮询、超时放弃等待。
2. 文档包含错误码映射与禁止项。
3. 文档给出可执行验证命令。

## 验证命令

```bash
# 1) 校验文档包含三项核心策略
grep -E "cancel token|轮询|放弃等待" docs/m0/m0-5-cancellation-strategy.md

# 2) 校验文档声明禁止线程强杀
grep -E "Thread.stop|禁止" docs/m0/m0-5-cancellation-strategy.md

# 3) 校验错误码映射
grep -E "PYTHON_CANCELLED|PYTHON_EXEC_TIMEOUT|PYTHON_INIT_TIMEOUT" docs/m0/m0-5-cancellation-strategy.md
```
