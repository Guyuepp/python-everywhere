## Plan: 安卓 Process Text + 本地 Python 详细任务清单

采用安卓原生 Process Text 作为系统级入口，在任意应用选中文字后调用本地 Python 执行，并以对话框式悬浮界面呈现结果。MVP 目标是稳定完成输入、执行、展示、复制 JSON 的端到端闭环，不引入无障碍和系统悬浮窗权限。

**Steps**
1. Milestone 0 - 项目与约束确认（Day 0，阻塞后续）
2. 任务 M0-1：确认技术栈与版本基线（Kotlin、Compose 或 View、Chaquopy、minSdk 26、targetSdk 当前稳定版）。完成定义：形成一页技术决策记录（ADR）。
3. 任务 M0-2：确认输入输出协议。完成定义：确定 JSON 字段与错误码字典，冻结 MVP 字段集合。
4. 任务 M0-3：定义非目标范围。完成定义：明确不做用户脚本管理、不做云端执行、不做系统悬浮窗权限。

5. Milestone 1 - 工程脚手架（Day 1，depends on M0）
6. 任务 M1-1：创建 Android 项目骨架。完成定义：空白应用可运行、可构建、签名配置可用。
7. 任务 M1-2：配置构建体系。完成定义：debug/release 两套构建变体可出包。
8. 任务 M1-3：接入依赖管理与基础质量工具（lint、ktlint 或 detekt 二选一）。完成定义：本地执行质量检查命令通过。

9. Milestone 2 - Python 运行时接入（Day 1-2，depends on M1）
10. 任务 M2-1：集成 Chaquopy 并跑通 Hello Python。完成定义：Kotlin 可调用 Python 并拿到字符串结果。
11. 任务 M2-2：实现 Python 统一入口函数 process_text。完成定义：函数接收字符串并返回结构化字典。
12. 任务 M2-3：实现 stdout/stderr 捕获。完成定义：示例脚本打印内容能分别落到 stdout 与 stderr 字段。
13. 任务 M2-4：异常收敛。完成定义：任何 Python 异常都转成统一错误对象（errorType、message、traceback）。
14. 任务 M2-5：执行超时。完成定义：超时后终止等待并返回 timeout 错误码。

15. Milestone 3 - Process Text 系统入口（Day 2-3，depends on M2）
16. 任务 M3-1：声明 Process Text Activity（intent-filter）。完成定义：安装后在支持该能力的第三方应用选中文本可看到菜单项。
17. 任务 M3-2：读取系统输入。完成定义：可获取选中文本与只读标记，并做空值处理。
18. 任务 M3-3：输入边界校验。完成定义：对超长文本、空白文本、异常编码有可预期提示与拒绝策略。
19. 任务 M3-4：线程模型落地。完成定义：Python 执行不阻塞主线程，UI 保持流畅无 ANR。

20. Milestone 4 - 结果展示层（Day 3，depends on M3）
21. 任务 M4-1：实现对话框式悬浮页面。完成定义：页面半透明遮罩 + 内容卡片，支持关闭。
22. 任务 M4-2：状态视图。完成定义：有执行中、成功、失败三态并显示耗时。
23. 任务 M4-3：详情视图。完成定义：可展开查看 stdout、stderr、traceback，长文本可滚动。
24. 任务 M4-4：可读性优化。完成定义：错误信息有短描述和展开详情两层。

25. Milestone 5 - JSON 打包与复制（Day 3-4，parallel with M4 late tasks）
26. 任务 M5-1：定义 ResultPayload 数据模型。完成定义：字段稳定且与 M0 协议一致。
27. 任务 M5-2：序列化与容错。完成定义：任意执行结果都能序列化成合法 JSON。
28. 任务 M5-3：复制到剪贴板。完成定义：点击复制后可在任意输入框粘贴完整 JSON。
29. 任务 M5-4：复制反馈。完成定义：成功与失败均有明确 Toast 或 Snackbar 提示。

30. Milestone 6 - 稳定性与安全（Day 4-5，depends on M4+M5）
31. 任务 M6-1：敏感数据处理。完成定义：release 默认不打印原始输入全文日志。
32. 任务 M6-2：崩溃兜底。完成定义：桥接层任何异常都不会导致应用崩溃退出。
33. 任务 M6-3：资源控制。完成定义：限制最大输入长度、限制执行时长、限制并发执行数为 1。
34. 任务 M6-4：兼容性策略。完成定义：对不支持 Process Text 的应用给出文档说明与替代路径（分享入口）。

35. Milestone 7 - 测试与验收（Day 5-6，depends on M6）
36. 任务 M7-1：单元测试（PythonRunner、ResultPayload）。完成定义：核心分支覆盖正常、异常、超时、空输入。
37. 任务 M7-2：仪器测试（入口 Activity）。完成定义：模拟 intent 传参可稳定拉起并产出结果。
38. 任务 M7-3：人工回归清单。完成定义：至少 3 类外部 App 回归通过（浏览器、阅读器、IM）。
39. 任务 M7-4：性能冒烟。完成定义：中等文本执行延迟在可接受阈值内（例如 P95 < 1.5s，按设备记录）。

40. Milestone 8 - 文档与发布准备（Day 6，depends on M7）
41. 任务 M8-1：编写用户文档。完成定义：含安装、使用、限制、故障排查。
42. 任务 M8-2：编写开发文档。完成定义：含构建命令、模块说明、调试开关。
43. 任务 M8-3：发布检查单。完成定义：签名、混淆、权限声明、隐私说明全部过一遍。
44. 任务 M8-4：MVP 验收评审。完成定义：通过“选中文本 -> 执行 -> 展示 -> 复制 JSON”端到端演示。

45. 并行建议
46. 可并行 A：M4-3 详情视图 与 M5-1 数据模型。
47. 可并行 B：M6-1 日志策略 与 M7-1 单元测试框架搭建。
48. 不可并行：M3 系统入口必须在 M2 Python 可调用后推进，否则联调成本高。

**Relevant files**
- ./README.md — 增加新子项目入口与使用说明。
- ./android-text-python-app/settings.gradle.kts — 工程与插件管理。
- ./android-text-python-app/build.gradle.kts — 顶层构建配置。
- ./android-text-python-app/app/build.gradle.kts — Android 与 Python 依赖配置。
- ./android-text-python-app/app/src/main/AndroidManifest.xml — Process Text 声明。
- ./android-text-python-app/app/src/main/java/.../ProcessTextActivity.kt — 接收输入与入口协调。
- ./android-text-python-app/app/src/main/java/.../PythonRunner.kt — 执行、超时、异常收敛。
- ./android-text-python-app/app/src/main/java/.../ResultPayload.kt — 统一结果模型。
- ./android-text-python-app/app/src/main/python/processor.py — Python 入口与输出捕获。
- ./android-text-python-app/app/src/test/... — 单元测试。
- ./android-text-python-app/app/src/androidTest/... — 仪器测试。

**Verification**
1. 构建验证：assembleDebug 与测试任务可跑通。
2. 入口验证：外部应用选中文字出现菜单项并可拉起。
3. 协议验证：复制出的 JSON 始终可解析，字段完整。
4. 错误验证：模拟脚本异常、超时、空输入，UI 不崩溃。
5. 体验验证：主线程无明显卡顿，关闭与返回行为符合预期。

**Decisions**
- 已定：系统级入口采用 Process Text。
- 已定：本地离线 Python，固定内置脚本。
- 已定：MVP 使用对话框式悬浮页面，不申请系统悬浮窗权限。
- 建议：最低 Android 8.0（API 26），兼顾覆盖率与开发效率。

**Further Considerations**
1. 二期若要支持多脚本，建议在 ResultPayload 预留 scriptId 与 scriptVersion。
2. 二期若要做真正系统悬浮窗，再单独引入 overlay 权限与兼容性策略。


## Plan v2: 安卓 Process Text + 本地 Python（风险修订版）

本版本吸收了关键工程风险：Intent 超大文本导致 Binder 事务失败、Chaquopy 冷启动卡顿、Python 不可强杀带来的取消难题、以及 AGP/Kotlin/Chaquopy 版本兼容地狱。MVP 目标不变，但技术路径与里程碑顺序已调整。

**Steps**
1. Milestone 0 - 技术预研与版本冻结（Day 0-1，阻塞后续）
2. 任务 M0-1：锁定版本矩阵（必须写死，不用 latest）。完成定义：ADR 中明确 AGP、Gradle、Kotlin、Chaquopy、Python 版本与已验证组合。
3. 任务 M0-2：做最小可行 Spike（空工程 + Chaquopy + 一次 Python 调用）。完成定义：本地与 CI 都可稳定构建。
4. 任务 M0-3：验证“超大 Intent”失败路径。完成定义：可复现实验并确认在读取 extra 前后都能被兜底，不崩溃。
5. 任务 M0-4：冻结结果协议与错误码。完成定义：新增错误码至少包含 INTENT_TOO_LARGE、PYTHON_INIT_TIMEOUT、PYTHON_EXEC_TIMEOUT、PYTHON_CANCELLED。
6. 任务 M0-5：确认取消策略技术路线。完成定义：采用“协作取消 + 放弃等待”模型，不做线程强杀。

7. Milestone 1 - 工程脚手架与预热（Day 1-2，depends on M0）
8. 任务 M1-1：创建工程骨架并配置 build variants。完成定义：debug/release 可出包。
9. 任务 M1-2：提前接入 Chaquopy 插件（从 M2 前移）。完成定义：首次构建可通过，记录构建时长与 APK 体积基线。
10. 任务 M1-3：引入自定义 Application。完成定义：应用进程启动后可进行 Python 环境预热（后台、限时、可降级）。
11. 任务 M1-4：预热策略。完成定义：冷启动时 Activity 不阻塞首屏，预热失败不影响主流程。

12. Milestone 2 - Python 执行引擎（Day 2-4，depends on M1）
13. 任务 M2-1：实现 Python 统一入口 process_text。完成定义：返回结构化对象 result/stdout/stderr/error/traceback。
14. 任务 M2-2：stdout/stderr 捕获与异常收敛。完成定义：异常均转统一结构。
15. 任务 M2-3：执行模型采用 Kotlin 协程 + Dispatchers.IO。完成定义：与 Activity 生命周期可绑定取消。
16. 任务 M2-4：安全取消机制（重点）。完成定义：Kotlin 侧发 cancel token，Python 侧轮询 token 并主动退出；超时后“放弃等待”并返回可解释状态。
17. 任务 M2-5：不可中断场景兜底。完成定义：若 Python 卡死，UI 可结束且后续请求被并发闸门保护，不引发崩溃。
18. 任务 M2-6：性能基线。完成定义：记录冷启动、温启动、平均执行时延。

19. Milestone 3 - Process Text 入口与生命周期（Day 4-5，depends on M2）
20. 任务 M3-1：Manifest 声明 PROCESS_TEXT 入口并确定 launchMode 策略。完成定义：说明 singleTop/singleTask 取舍，若复用实例则实现 onNewIntent 冲突处理。
21. 任务 M3-2：Intent 读取最外层兜底（重点）。完成定义：onCreate 早期对读取 extra 全程 try-catch，捕获 TransactionTooLargeException/RuntimeException 并降级提示，不崩溃。
22. 任务 M3-3：输入边界校验。完成定义：空值、空白、超长、异常编码均有可预期结果。
23. 任务 M3-4：执行请求串行化。完成定义：同一时刻只允许一个有效执行，后续请求可排队或拒绝并提示。

24. Milestone 4 - 结果界面与交互（Day 5，depends on M3）
25. 任务 M4-1：对话框式结果页。完成定义：执行中/成功/失败三态与耗时展示齐全。
26. 任务 M4-2：关闭事件与执行联动（重点）。完成定义：用户关闭页面仅触发协作取消，不做线程硬终止。
27. 任务 M4-3：详情区域。完成定义：stdout/stderr/traceback 可展开、可滚动。

28. Milestone 5 - JSON 复制与可观测性（Day 5-6，depends on M4）
29. 任务 M5-1：ResultPayload 模型与序列化。完成定义：任意状态都能输出合法 JSON。
30. 任务 M5-2：一键复制与反馈。完成定义：复制成功可粘贴验证，失败有错误提示。
31. 任务 M5-3：诊断字段。完成定义：JSON 包含版本号、设备信息、是否冷启动、耗时分段。

32. Milestone 6 - 稳定性、安全与测试（Day 6-8，depends on M5）
33. 任务 M6-1：日志脱敏。完成定义：release 默认不落原文敏感内容。
34. 任务 M6-2：单元测试。完成定义：覆盖正常/异常/超时/取消/intent 读取失败。
35. 任务 M6-3：仪器测试。完成定义：Process Text 拉起、onNewIntent、复制 JSON 流程通过。
36. 任务 M6-4：人工回归。完成定义：至少 3 类外部应用完成回归。

37. Milestone 7 - 发布准备（Day 8-9，depends on M6）
38. 任务 M7-1：发布检查单。完成定义：权限、隐私、签名、混淆、体积、性能全部过线。
39. 任务 M7-2：文档。完成定义：用户指南 + 开发指南 + 已知限制（包含部分 App 不暴露 Process Text 菜单）。

**Key Design Rules（新增强制项）**
1. Intent extra 读取必须放在 onCreate 早期，外层全包 try-catch。
2. 不允许使用 Thread.stop/interruption 强杀 Python 执行线程。
3. 取消机制必须是协作式，超时策略是“停止等待 + 状态回传 + 并发闸门”。
4. 版本矩阵必须锁死，禁止自动升级 AGP/Kotlin/Chaquopy。
5. Chaquopy 插件前移到脚手架阶段，避免后期集中爆雷。

**Verification（更新）**
1. Binder 压力测试：构造极大文本触发失败，确认应用不崩溃并返回可读错误。
2. 冷启动体验测试：首次触发 Process Text 到结果页时间在可接受阈值内。
3. 取消一致性测试：关闭页面、超时、重复触发三种路径无崩溃无脏状态。
4. 兼容性测试：不同 launchMode 下多次触发行为一致。

**Decisions（更新）**
- 工期由 Day 0-6 调整为 Day 0-9（含 Chaquopy 风险缓冲）。
- M1 前置 Chaquopy 与预热，M2 专注执行引擎与取消语义。
- M3 强化 Intent 早期兜底与生命周期冲突处理。
