# M3-1: Process Text 入口声明与 launchMode 策略

- 里程碑: Plan v2 / M3-1
- 目标: 声明 Process Text 入口，并明确复用实例时的 Intent 冲突处理。

## 入口声明

`MainActivity` 新增 `PROCESS_TEXT` intent-filter：

1. action: `android.intent.action.PROCESS_TEXT`
2. category: `android.intent.category.DEFAULT`
3. mimeType: `text/plain`

## launchMode 取舍

推荐方案: `singleTop`

原因：

1. 相比 `singleTask`，任务栈行为更可控，不会引入跨任务栈副作用。
2. 当顶部已有同 Activity 实例时，通过 `onNewIntent` 复用并处理新请求，减少实例抖动。
3. 能以最小改动实现“新请求到来时取消旧请求并切换到新请求”。

## 冲突处理策略

1. `onNewIntent` 收到新请求后调用 `setIntent(newIntent)`。
2. 取消旧请求（协作取消），并取消旧协程任务。
3. 基于新 Intent 重新创建 requestId 并发起执行。

## 验证命令

```bash
cd android-text-python-app
./gradlew :app:assembleDebug

# 验证 manifest 声明
grep -n "PROCESS_TEXT\|launchMode=\"singleTop\"" app/src/main/AndroidManifest.xml
```
