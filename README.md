# WeKit

WeKit 修改版

## 修改内容

- 添加 WAuxiliary 与 NewMiko 目前公开源代码中的部分功能
- 移除全部校验，减少模块体积，避免不必要性能开销
- 移植大部分 UI 至 Jetpack Compose
- 添加 WAuxiliary 部分闭源功能
- 一些功能

## 特色功能

- 基于 JavaScript 的脚本自动化引擎, [API 文档](SCRIPT_API_DOCUMENT.md)
- 贴纸包同步（Telegram Stickers Sync）

## Q&A

1. - Q: 我的微信突然卡得要死，狂吃内存
   - A：禁用“Xposed API 调用保护”和“隐藏应用列表”（我原以为删了动态加载就可以开着这俩，调试的时候被坑死了）
2. - Q: XXX
   - A: 找 [DeepWiki](https://deepwiki.com/Ujhhgtg/WeKit)

## 注意

一切开发旨在学习，请勿用于非法用途

使用本项目的源代码必须遵守 GPL-3.0 许可证，详见 LICENSE

使用本模块代码或分发修改版时**必须**继续以 GPL-3.0 协议开源

## 致谢

[WeKit](https://github.com/cwuom/WeKit)

[WAuxiliary (公开代码)](https://github.com/HdShare/WAuxiliary_Public)

[WAuxiliary (反混淆)](https://github.com/Ujhhgtg/wauxv_deobf)

[NewMiko (公开代码)](https://github.com/dartcv/NewMiko/blob/archives/)
