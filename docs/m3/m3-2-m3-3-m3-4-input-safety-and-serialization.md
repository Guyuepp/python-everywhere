# M3-2 / M3-3 / M3-4：输入兜底、边界校验与串行执行

- 里程碑: Plan v2 / M3-2, M3-3, M3-4
- 目标: 输入读取不崩溃、输入边界有统一错误返回、执行串行且主线程不阻塞。

## M3-2 最外层兜底

1. 在 `onCreate` 与 `onNewIntent` 的统一入口 `startExecutionFromIntent` 中读取输入。
2. 输入解析路径最外层 `try-catch` 捕获异常。
3. 对疑似 Binder 过大异常映射 `INTENT_TOO_LARGE`，其他读取异常映射 `INVALID_INPUT`。
4. 失败时直接渲染统一错误返回，不抛出异常导致崩溃。

## M3-3 输入边界校验

`InputValidator` 规则：

1. 空值: `INVALID_INPUT`
2. 全空白: `INVALID_INPUT`
3. 超长（> 10000 chars）: `INVALID_INPUT`
4. 异常内容（NUL、非法控制字符、U+FFFD）: `INVALID_INPUT`

## M3-4 串行执行与主线程流畅

1. `PythonRunner` 维持单执行闸门，限制同一时刻有效执行数为 1。
2. 请求执行在协程 IO 线程，主线程仅做状态渲染，不阻塞首屏。
3. 为替换请求场景提供限时闸门等待，避免闸门释放窗口导致误判 busy。

## 验证命令

```bash
cd android-text-python-app
./gradlew :app:assembleDebug
./gradlew :app:lint
./gradlew :app:testDebugUnitTest
```
