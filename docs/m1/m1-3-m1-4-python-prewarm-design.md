# M1-3 / M1-4 设计说明：Python 预热与首屏非阻塞

- 范围: Plan v2 的 M1-3、M1-4
- 原则: 最小改动、可验证、不改变 M0 错误码语义、不引入线程强杀。

## 设计要点

1. 自定义 `Application` 启动后台预热：
   - 入口: `PythonSpikeApplication.onCreate`。
   - 行为: 异步触发 `PythonPrewarmer.schedule`，不阻塞主线程。
2. 预热限时策略：
   - 预热任务由后台线程执行。
   - 看门狗线程等待固定超时（2.5s），超时后仅“放弃等待并记录状态”，不强杀线程。
3. 主流程兜底：
   - `MainActivity` 首帧先渲染 `running...`，然后在后台线程执行 Python。
   - 若预热失败/超时，主流程调用 `PythonRuntime.ensureStarted` 兜底启动，不阻塞首屏。
4. 日志策略：
   - debug 下打印关键状态（预热开始、成功、超时、调用完成）。
   - release 下默认不打印敏感输入内容，仅保留必要告警摘要。
5. 协议字段来源：
   - `isColdStart` 来源已在 M0 协议文档补充，按执行前 `Python.isStarted()` 快照判定。

## 完成定义 (Definition of Done)

1. Manifest 挂载自定义 Application。
2. 应用启动触发后台限时预热，超时不阻塞主流程。
3. Activity 首屏先显示，再后台执行 Python，避免首屏阻塞。
4. M0 协议文档补充 `isColdStart` 字段来源说明。
5. Debug 可见日志存在，且未打印原始敏感输入。

## 验证命令

```bash
# 1) 编译通过
cd android-text-python-app
./gradlew :app:assembleDebug

# 2) 检查 Application 挂载
grep -n "android:name=\".PythonSpikeApplication\"" app/src/main/AndroidManifest.xml

# 3) 检查预热与兜底关键实现
grep -n "class PythonSpikeApplication\|object PythonPrewarmer\|PREWARM_TIMEOUT_MS\|ensureStarted" app/src/main/java/com/example/pythonspike/*.kt

# 4) 检查首屏非阻塞（先渲染 running...）
grep -n "running...\|worker.execute" app/src/main/java/com/example/pythonspike/MainActivity.kt

# 5) 检查协议文档 isColdStart 来源说明
grep -n "预热状态字段来源说明\|isColdStart" ../docs/m0/m0-4-result-protocol.md
```
