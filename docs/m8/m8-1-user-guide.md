# M8-1 用户文档（安装、使用、限制、故障排查）

- 里程碑: Plan / M8-1
- 适用版本: 1.0 (debug 验证基线)
- 测试设备参考: Android 16 / OnePlus Ace 6

## 1. 产品说明

本应用通过系统 Process Text 能力，在任意支持该菜单的应用中选中文本后，调用本地 Python 执行并展示结果。
从启动器打开应用时，默认进入历史记录页，用于查看最近执行记录。

核心链路:
1. 选中文本。
2. 触发系统菜单项 Run as Python。
3. 查看执行状态与结果。
4. 一键复制结果 JSON。
5. 从启动器进入历史记录页查看最近结果。

## 2. 安装说明

### 2.1 通过开发环境安装（推荐开发测试）

1. 连接 Android 设备并开启 USB 调试。
2. 在项目目录执行:

```bash
cd android-text-python-app
./gradlew :app:installDebug
```

3. 安装后在系统应用列表中可见 Python Spike。

### 2.2 安装验证

1. 打开应用，默认显示历史记录页。
2. 在支持 Process Text 的应用中选中文本，菜单项中可见 Run as Python。

## 3. 使用说明

### 3.0 启动器查看历史

1. 从桌面点击 Python Spike。
2. 进入 Execution History 页面，可查看最近执行记录。
3. 历史最多保留最近 100 条，超出会自动淘汰最旧记录。
4. 可点击 Clear 清空历史（需二次确认）。

### 3.1 基本执行

1. 在外部应用中选中文本，例如 `1+2*3`。
2. 点击系统菜单 Run as Python。
3. 结果页会显示 Running -> Success/Error。
4. 成功时可看到结果 `7`。

### 3.2 错误输入

1. 输入非 Python 代码（如 `hello world`）。
2. 结果页应显示 Error。
3. 可展开 traceback 查看具体错误信息。

### 3.3 复制结果 JSON

1. 执行完成后点击 Copy Result JSON。
2. 在任意输入框粘贴，得到可解析 JSON。
3. JSON 至少包含字段:
   - protocol
   - status
   - message
   - result
   - stdout
   - stderr
   - traceback
   - durationMs

## 4. 结果字段说明（简版）

1. status:
   - success: 执行成功
   - error: 执行失败
   - cancelled: 被取消
2. errorCode:
   - success 通常为 null
   - error/cancelled 时为具体错误码
3. durationMs:
   - 应用内端到端执行耗时（Elapsed）
4. stdout/stderr/traceback:
   - 分别对应标准输出、标准错误和异常堆栈

## 5. 已知限制

1. 部分应用不暴露系统 Process Text 菜单，无法直接拉起本应用。
2. 当前实测中，部分 IM 应用（微信、Telegram、QQ）存在该限制。
3. 当来源应用本身不提供选中文本或拦截系统菜单时，本应用无法介入。
4. 历史仅记录来自 Process Text 的执行，不记录其他来源路径。

## 6. 替代使用路径

当目标应用不支持 Process Text 时，可用以下方式:
1. 将文本复制到支持 Process Text 的应用（浏览器、阅读器、系统编辑器）。
2. 在该应用中选中文本并执行 Run as Python。
3. 复制 JSON 后再返回原应用使用。

## 7. 故障排查

### 7.1 看不到 Run as Python 菜单

可能原因:
1. 来源应用不支持系统 Process Text。
2. 当前选区不是可处理的纯文本。
3. 应用未正确安装或被系统禁用。

处理步骤:
1. 先在浏览器或阅读器中测试是否可见菜单。
2. 确认应用已安装最新版本。
3. 重启来源应用后重试。

### 7.2 显示 Error

可能原因:
1. 输入内容不是合法 Python 代码。
2. 代码运行抛出异常。
3. 输入包含异常字符或超出边界约束。

处理步骤:
1. 先用 `1+2*3` 验证基础链路。
2. 展开 traceback 定位错误。
3. 修正代码后再次执行。

### 7.3 复制 JSON 失败

可能原因:
1. 执行尚未完成（无可复制结果）。
2. 系统剪贴板被其他应用短时占用。

处理步骤:
1. 等待状态变为 Success 或 Error 再复制。
2. 重试一次并立即粘贴确认。

### 7.4 执行卡顿或超时

可能原因:
1. 输入代码复杂度较高。
2. 设备高负载或后台资源紧张。

处理步骤:
1. 先执行简单表达式确认链路。
2. 关闭后台高负载应用后重试。
3. 拆分大段代码分步执行。

## 8. 反馈与问题上报建议

问题上报建议附带:
1. 设备型号与 Android 版本。
2. 来源应用名称与版本。
3. 输入样例（可脱敏）。
4. 结果 JSON（成功或失败）。
5. 相关日志片段（PythonSpike / AndroidRuntime）。
