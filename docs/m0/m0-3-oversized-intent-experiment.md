# M0-3: 超大 Intent 失败复现实验与兜底说明

- 里程碑: Plan v2 / M0-3
- 目标: 可复现超大 Intent 失败，并明确异常分类与降级策略，确保应用不崩溃。

## 实验前置条件

1. 已连接 Android 设备或模拟器 (`adb devices` 至少出现 1 台 `device`)。
2. 已安装 Debug 包。
3. 应用包名为 `com.example.pythonspike`（当前 Spike 默认值）。

## 实验步骤

### 步骤 A: 复现“发送阶段失败”（进入应用前）

1. 生成超大文本（约 1.2MB）。
2. 通过 `am start` 发送到应用 Activity。

```bash
PAYLOAD=$(python3 - <<'PY'
print('A' * 1200000)
PY
)

adb logcat -c
adb shell am start -n com.example.pythonspike/.MainActivity --es android.intent.extra.PROCESS_TEXT "$PAYLOAD"
adb logcat -d | grep -Ei "TransactionTooLargeException|FAILED BINDER TRANSACTION|Transaction too large"
```

### 步骤 B: 复现“读取阶段失败”（进入应用后）

1. 使用较小文本启动，验证 Activity 能正常进入。
2. 在实际 Process Text 入口 Activity 落地后，保持 `onCreate` 最外层读取 intent/extras 的 try-catch。
3. 对超大或异常 extras 触发后，观察是否进入统一错误回传，而不是崩溃退出。

```bash
adb logcat -c
adb shell am start -n com.example.pythonspike/.MainActivity --es android.intent.extra.PROCESS_TEXT "hello"
adb logcat -d | grep -Ei "RuntimeException|BadParcelableException|TransactionTooLargeException"
```

## 预期现象

1. 步骤 A 可能在系统层直接失败，典型日志为 Binder 事务过大，应用进程可能未进入业务代码。
2. 步骤 B 在后续 Process Text Activity 实装后，若 extras 读取失败，应被 try-catch 捕获并降级，不出现闪退。

## 异常分类

1. 发送阶段异常（App 前）:
   - `TransactionTooLargeException`
   - `FAILED BINDER TRANSACTION`
2. 读取阶段异常（App 内）:
   - `TransactionTooLargeException`
   - `RuntimeException`（由 Bundle/Parcel 读取触发）
   - `BadParcelableException`

## 降级策略

1. Intent 读取最外层兜底:
   - 在 `onCreate` 早期读取 intent/extras。
   - 外层统一 `try-catch`，捕获上述异常类型及其父类运行时异常。
2. 协议化回传:
   - 统一映射为错误码 `INTENT_TOO_LARGE`。
   - UI 展示简短可读文案，详情可展开日志摘要。
3. 资源与状态控制:
   - 不进入 Python 初始化与执行链路。
   - 不重试超大请求，直接返回失败态。

## 完成定义 (Definition of Done)

1. 文档化两类可复现实验路径（发送阶段失败、读取阶段失败）。
2. 文档化预期现象、异常分类与降级策略。
3. 明确错误码映射到 `INTENT_TOO_LARGE`。

## 验证命令

```bash
# 1) 构建并安装
cd android-text-python-app
./gradlew :app:assembleDebug

# 2) 查看设备
adb devices

# 3) 触发超大 Intent 复现
PAYLOAD=$(python3 - <<'PY'
print('A' * 1200000)
PY
)
adb shell am start -n com.example.pythonspike/.MainActivity --es android.intent.extra.PROCESS_TEXT "$PAYLOAD"

# 4) 检查日志关键字
adb logcat -d | grep -Ei "TransactionTooLargeException|FAILED BINDER TRANSACTION|Transaction too large"
```
