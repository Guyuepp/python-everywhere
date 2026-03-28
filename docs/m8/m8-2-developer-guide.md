# M8-2 开发文档（构建命令、模块说明、调试开关）

- 里程碑: Plan / M8-2
- 项目目录: `android-text-python-app`
- 文档目标: 帮助开发者快速完成构建、调试、测试与问题定位。

## 1. 环境与版本基线

当前工程锁定版本（禁止 latest / 浮动版本）:
1. AGP: 9.1.0
2. Gradle Wrapper: 9.3.1
3. Kotlin Gradle Plugin: 2.3.20
4. Chaquopy Plugin: 17.0.0
5. Python Runtime (Chaquopy): 3.14

参考位置:
1. `android-text-python-app/build.gradle.kts`
2. `android-text-python-app/app/build.gradle.kts`
3. `android-text-python-app/gradle/wrapper/gradle-wrapper.properties`
4. `docs/adr/0001-version-matrix-baseline.md`

## 2. 项目结构

```text
android-text-python-app/
  app/
    src/main/java/com/example/pythonspike/
      MainActivity.kt
      PythonRunner.kt
      PythonBridge.kt
      ProcessTextIntentReader.kt
      ResultPayload.kt
      ...
    src/main/python/
      processor.py
    src/test/java/com/example/pythonspike/
      PythonRunnerTest.kt
      ResultPayloadTest.kt
      InputValidatorTest.kt
      ProcessTextIntentReaderTest.kt
    src/androidTest/java/com/example/pythonspike/
      MainActivityProcessTextTest.kt
```

## 3. 核心模块说明

1. MainActivity
- 职责: Process Text 入口、输入解析、状态渲染、复制 JSON。
- 关键点: `onNewIntent` 新请求覆盖旧请求；Elapsed 显示端到端执行耗时。

2. ProcessTextIntentReader
- 职责: 安全读取 Intent 输入，处理读取异常并映射错误码。
- 关键点: 对大事务失败映射 `INTENT_TOO_LARGE`。

3. InputValidator
- 职责: 输入边界校验（空白、超长、异常字符）。
- 关键点: 拒绝非法输入并返回 `INVALID_INPUT`。

4. PythonRunner
- 职责: 协程调度、超时控制、并发闸门、取消语义。
- 关键点:
  - 单并发执行。
  - 超时策略为“放弃等待 + 状态回传”。
  - 不做线程硬终止。

5. PythonBridge (ChaquopyPythonBridge)
- 职责: Kotlin 与 Python 的桥接调用。
- 关键点: 调用 `processor.process_text`，并提供 cancel/clear 接口。

6. processor.py
- 职责: 执行 Python 代码、捕获 stdout/stderr、统一结构返回。
- 关键点:
  - 输入按 Python 代码执行。
  - 非法代码进入 error 路径。
  - 输出结构与 Kotlin 侧协议一致。

7. ResultPayload / PythonResultPayload
- 职责: 结果模型统一与 JSON 序列化。
- 关键点: 任意状态都可得到合法 JSON。

## 4. 常用命令

在 `android-text-python-app` 目录执行。

### 4.1 构建

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

### 4.2 单元测试

```bash
./gradlew :app:testDebugUnitTest
```

### 4.3 仪器测试

```bash
./gradlew :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

### 4.4 安装到设备

```bash
./gradlew :app:installDebug :app:installDebugAndroidTest
```

### 4.5 快速看设备状态

```bash
adb devices
```

## 5. 调试开关与测试输入

通过 Intent extra 可注入调试参数:
1. `debug_delay_ms`:
- 类型: Long
- 作用: 人为增加执行延迟，用于覆盖/超时测试。

2. `debug_fail`:
- 类型: Boolean
- 作用: 强制触发异常路径。

示例（adb）:
```bash
adb shell am start -W \
  -n com.example.pythonspike/.MainActivity \
  -a android.intent.action.PROCESS_TEXT \
  --es android.intent.extra.PROCESS_TEXT "1+2*3" \
  --el debug_delay_ms 0
```

## 6. 日志与诊断

推荐日志过滤:
```bash
adb logcat -d | grep -E "PythonSpike|AndroidRuntime" | tail -n 200
```

日志要点:
1. `PythonSpike/MainActivity`: 入口、渲染、生命周期。
2. `PythonSpike/PythonRunner`: 执行开始/完成、超时、取消。
3. `PythonSpike/PerfBaseline`: 基线统计（warm/cold 计数与耗时）。

## 7. 测试覆盖现状

1. JVM 单测:
- `PythonRunnerTest`
- `ResultPayloadTest`
- `InputValidatorTest`
- `ProcessTextIntentReaderTest`

2. 仪器测试:
- `MainActivityProcessTextTest`（Process Text 拉起、onNewIntent 覆盖、复制 JSON）

## 8. 已知限制（开发视角）

1. 部分 IM 不暴露系统 Process Text 菜单（微信/Telegram/QQ）。
2. 该限制属于来源应用策略，不是本应用执行引擎故障。
3. 回归时建议优先使用浏览器/阅读器等支持 Process Text 的应用。

## 9. 常见问题

1. `connectedDebugAndroidTest` 失败但用例逻辑看起来正确:
- 先看是否是 Activity 清理阶段问题（生命周期未销毁）。
- 复跑单个用例并抓日志确认。

2. `Error type 3`（Activity 不存在）:
- 先确认包已安装。
- 重新执行 `installDebug`。

3. adb 命令包含分号报 shell 语法错误:
- 避免在 `--es` 文本里直接使用未转义分号。
- 优先使用简单表达式样例验证。

## 10. 开发建议

1. 变更执行语义时，同步更新:
- `processor.py`
- 仪器测试样例
- 用户文档与回归文档

2. 提交前最低检查:
```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
```
