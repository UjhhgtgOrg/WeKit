# 脚本 API 文档

## Beta 状态警告

**重要提醒：此 API 目前处于 Beta 测试阶段，在后续更新中可能出现不兼容的更改，使用时请密切关注相关更新通知。**
## 目录

1. [钩子函数](#钩子函数)
    - [onRequest](#onrequest)
    - [onResponse](#onresponse)
2. [WEKit 对象](#wekit-对象)
    - [概述](#概述)
    - [WEKit Log 函数](#wekit-log-函数)

## 钩子函数

### onRequest

当请求即将发出时触发此函数。

```javascript
function onRequest(data) {
    // data 对象包含以下字段：
    const {uri,cgiId,jsonData} = data;

    // 示例：修改请求数据
    if (data.cgiId === '111') {
        data.jsonData.newField = 'newValue';
    }
}
```

### onResponse

当请求收到响应时触发此函数。

```javascript
function onResponse(data) {
    // data 对象包含以下字段：
    const {uri,cgiId,jsonData} = data;

    // 示例：修改响应数据
    if (data.cgiId === 'some_cgi_id') {
        data.jsonData.newField = 'newValue';
    }
}
```

## 数据对象说明

每个钩子函数接收一个 `data` 参数，该参数是一个对象，包含以下字段：

| 字段名   | 类型   | 描述                         |
|----------|--------|------------------------------|
| uri      | string | 请求的目标 URI 地址          |
| cgiId    | string | 请求的 CGI ID，用于识别请求类型 |
| jsonData | object | 请求或响应的数据体（JSON 格式） |

## WEKit 对象

### 概述

`wekit` 是一个特殊的全局对象，是软件为脚本环境设计的api，提供了与底层系统交互的各种功能接口。该对象在脚本执行环境中自动可用，无需额外导入或初始化。

### WEKit Log 函数

`wekit.log` 是 `wekit` 对象提供的日志输出函数，专门用于在脚本执行过程中打印调试信息。所有通过此函数输出的日志都会被收集并可在脚本日志查看器中查看。

#### 使用方法

```javascript
wekit.log(message);
```

#### 示例

```javascript
function onRequest(data) {
    wekit.log('请求发起:', data.uri);
    wekit.log('CGI ID:', data.cgiId);
    wekit.log('原始数据:', JSON.stringify(data.jsonData));
    
    // 修改数据...
}
```

### 注意事项

1. 日志输出将显示在脚本日志查看器中
2. 当 API 发生变更时，请及时更新相关脚本代码

### 后续更新计划

我们可能在稳定版本中提供更加完善和一致的日志功能接口，届时会提供更详细的文档和更稳定的 API 设计。