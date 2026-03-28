# M2-6: 性能基线（冷启动/温启动/平均耗时）

- 里程碑: Plan v2 / M2-6
- 目标: 建立可重复的执行耗时基线，覆盖冷启动、温启动、平均耗时。

## 当前实现

1. 在 `PythonRunner` 记录每次请求耗时（ms）与 `isColdStart`。
2. 通过 `PerformanceBaselineTracker` 聚合以下指标：
   - `coldCount`
   - `warmCount`
   - `avgDurationMs`
   - `coldAvgDurationMs`
   - `warmAvgDurationMs`
3. debug 日志输出聚合快照，不包含敏感输入原文。

## 基线采集步骤

1. 冷启动样本：
   - `adb shell am force-stop com.example.pythonspike`
   - 启动一次执行请求，记录日志中的 `PerfBaseline`。
2. 温启动样本：
   - 不 force-stop，连续触发多次请求。
   - 记录 `PerfBaseline` 日志中的 warm 指标。
3. 平均耗时：
   - 使用日志里的 `avgMs` 作为当前会话平均耗时。

## 验证命令

```bash
cd android-text-python-app
./gradlew :app:assembleDebug

# 若有设备
adb shell am force-stop com.example.pythonspike
adb logcat -c
adb shell am start -n com.example.pythonspike/.MainActivity
adb logcat -d | grep "PerfBaseline"
```
