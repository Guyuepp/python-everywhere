# M4-1 ~ M4-3: 对话框式结果页（三态 + 耗时 + 可展开详情）

- 里程碑: Plan v2 / M4-1, M4-2, M4-3
- 目标: 使用对话框式 UI 展示执行中/成功/失败状态，显示耗时，并可展开查看 stdout/stderr/traceback。

## 关键实现

1. 对话框式结果页：
   - 使用半透明遮罩 + 居中白色卡片布局。
2. 三态展示：
   - 执行中: 显示 Running 与进度条。
   - 成功: 显示 Success。
   - 失败: 显示 Failed（包含取消/异常）。
3. 耗时展示：
   - 每次请求结束后展示 `Elapsed: X ms`。
4. 详情区域：
   - stdout/stderr/traceback 分区，可点击展开/收起。
   - 每个分区使用 ScrollView，支持长文本滚动。

## 验证命令

```bash
cd android-text-python-app
./gradlew :app:assembleDebug
./gradlew :app:lint
./gradlew :app:testDebugUnitTest
```
