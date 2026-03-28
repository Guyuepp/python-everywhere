# M0-2: 最小可行 Spike（空 Android 工程 + Chaquopy + 一次 Python 调用）

- 里程碑: Plan v2 / M0-2
- 目标: 用最小链路验证 Android 工程可调用内置 Python，并产出可见结果。

## Spike 范围

1. Android 应用骨架可构建、可安装。
2. 已接入 Chaquopy 插件。
3. Activity 启动后触发一次 Python 调用并显示结果。

## 关键落地点

1. 版本矩阵固定（禁止 latest）：
   - [docs/adr/0001-version-matrix-baseline.md](docs/adr/0001-version-matrix-baseline.md)
2. Chaquopy 插件与 Python 版本：
   - [android-text-python-app/build.gradle.kts](android-text-python-app/build.gradle.kts)
   - [android-text-python-app/app/build.gradle.kts](android-text-python-app/app/build.gradle.kts)
3. Python 入口函数：
   - [android-text-python-app/app/src/main/python/processor.py](android-text-python-app/app/src/main/python/processor.py)
4. Kotlin 到 Python 调用桥接：
   - [android-text-python-app/app/src/main/java/com/example/pythonspike/PythonBridge.kt](android-text-python-app/app/src/main/java/com/example/pythonspike/PythonBridge.kt)

## 最小调用说明

1. 当 Activity 非 Process Text 启动时，默认输入值为 Python 字面量 `'hello'`。
2. 默认输入改为 Python 字面量 `'hello'`，Python 返回结果 text=hello。
3. UI 在成功态显示该结果，并可复制 JSON 负载。

## 验证命令

```bash
cd android-text-python-app
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
```

## 完成定义

1. 构建通过，说明工程与 Chaquopy 组合可用。
2. 单测通过，说明最小调用路径可回归。
3. 可打出 androidTest 包，为后续 UI 仪器测试提供基线。
