# WeKit

适用于微信的 Xposed 模块

## 修改内容

- 添加 WAuxiliary 与 NewMiko 目前公开源代码中的部分功能
- 移除全部校验, 减少模块体积, 避免不必要性能开销 (注意: 签名无论 Release/Debug 均使用默认 Debug 签名, 请勿从不安全来源安装模块)
- 移植绝大部分 UI 至 Jetpack Compose
- 添加, 修复, 增强 WAuxiliary 部分闭源功能
- 一些功能

## 推荐版本

- 非 Play: 8.0.67~8.0.68
- Play: 未测试

## 特色功能

- 基于 JavaScript 的脚本自动化引擎, [API ~~文档~~参考](app/src/main/java/moe/ouom/wekit/hooks/items/automation/globals.d.ts)
- 贴纸包同步 (Telegram Stickers Sync)
- 通知进化
- 引用消息直达 (支持新版微信)

## Q&A

1. - Q: 我的微信突然卡得要死, 狂吃内存
   - A：禁用 'Xposed API 调用保护' 和 '隐藏应用列表' (我原以为删了动态加载就可以开着这俩，调试的时候被坑死了)
2. - Q: XXX
   - A: 找 [DeepWiki](https://deepwiki.com/Ujhhgtg/WeKit)

## 注意

一切开发旨在学习，请勿用于非法用途

使用本项目的源代码必须遵守 GPL-3.0 许可证，详见 LICENSE

使用本模块代码或分发修改版时**必须**继续以 GPL-3.0 协议开源

## 贡献须知

本 Fork 接受从其他模块提取的功能

编写 UI 时请尽量使用 Jetpack Compose, 如果你还不会用, 那你真的应该去学习一下, 很适合模块 UI

提交 PR 前请确保可以通过编译, 功能正常, 不影响其他功能

## 已知问题

- 每次 DEX 解析缓存清空后, WeMessageApi.classChattingContext 第一次将会必定解析失败, 重启应用重试即可恢复正常, 原因暂时未知

## lol

```none
关于上游某 PR “引用” WA 逻辑与本 Fork 的几点说明

听说最近有人因为 WeKit 的某个 PR (Script Manager Click Crash Fix) 破防了？理由是里面“参考”了 WA 的逆向成果。针对此事，我已第一时间进行了核实与嘲笑处理，并在此澄清我的立场： 

1. 关于“为何抄袭”

为了保证本 Fork 的纯粹功能性，我个人天天接触、也经常查阅 WA 项目的大量源代码。正因如此，在编写本项目代码时，我处于完全的“信息明区”，我加入了大量 WAuxiliary 的功能。

2. 处理动作

目前，我已经忽略了上游所有 Revert（撤回）提交。本项目完全接受任何通过“搬运”他方成品而来的贡献。

3. 关于后续开发

某些人似乎产生了一种幻觉：觉得某个 Method 只要被他 Hook 了，那个 Offset 就改姓了。
真相是：微信的函数点位是客观存在的物理事实，不是谁的私产。
我的态度：我之后会继续开发。如果我的代码恰好和你的一样，那你受着呗。

4. 总结

这次事件对我是一个乐子：开源的本质是分享，而逆向工程的本质是拆解黑盒。一个靠拆别人黑盒起家的项目，却忙着给自己套上黑盒，并对后来的拆解者指手画脚，有点好笑了。

本 Fork 继续走他人实现、自己抄袭的路。
```

## 致谢

[WeKit](https://github.com/cwuom/WeKit)

[WAuxiliary (公开代码)](https://github.com/HdShare/WAuxiliary_Public)

[WAuxiliary (反混淆)](https://github.com/Ujhhgtg/wauxv_deobf)

[NewMiko (公开代码)](https://github.com/dartcv/NewMiko/blob/archives/)
