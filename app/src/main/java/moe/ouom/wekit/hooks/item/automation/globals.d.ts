/**
 * 脚本全局 API 定义
 */

interface HttpResponse {
    /** 请求是否成功 (status < 400) */
    ok: boolean;
    /** HTTP 状态码 */
    status: number;
    /** 响应体字符串 */
    body: string;
    /** 如果响应体是 JSON，则自动解析好的对象；否则为 null */
    json: any | null;
    /** 错误信息（如果有） */
    error?: string;
}

interface DownloadResult {
    /** 下载是否成功 */
    ok: boolean;
    /** 文件保存的绝对路径 */
    path: string;
}

declare namespace log {
    function d(message: any): void;
    function i(message: any): void;
    function w(message: any): void;
    function e(message: any): void;
}

declare namespace http {
    /** 发送 GET 请求 */
    function get(url: string, params?: object, headers?: object): HttpResponse;
    /** 发送 POST 请求 */
    function post(url: string, formData?: object, jsonBody?: object, headers?: object): HttpResponse;
    /** 下载文件到宿主缓存目录 */
    function download(url: string): DownloadResult;
}

declare namespace storage {
    /** 获取指定键的值。若不存在返回 undefined。 */
    function get(key: string): any;
    /** 获取值，若不存在则返回提供的默认值 def。 */
    function getOrDefault(key: string, def: any): any;
    /** 存入键值对。若 value 为 undefined 则移除该键。 */
    function set(key: string, value: any): void;
    /** 判断是否存在指定的键。 */
    function hasKey(key: string): boolean;
    /** 移除指定的键。 */
    function remove(key: string): void;
    /** 取出并立即移除该键。若不存在返回 undefined。 */
    function pop(key: string): any;
    /** 返回包含所有键名的字符串数组。 */
    function keys(): string[];
    /** 返回当前存储条目总数。 */
    function size(): number;
    /** 清空所有键值对。 */
    function clear(): void;
    /** 检查存储是否为空。 */
    function isEmpty(): boolean;
}

// --- 消息发送 API (指定接收人) ---

/** 向指定用户发送文本消息 */
declare function sendText(to: string, text: string): void;
/** 向指定用户发送图片消息 */
declare function sendImage(to: string, path: string): void;
/** 向指定用户发送文件消息 */
declare function sendFile(to: string, path: string, title?: string): void;
/** 向指定用户发送语音消息 */
declare function sendVoice(to: string, path: string, durationMs: number): void;

// --- 消息回复 API (自动回复至发送者) ---

/** 回复文本消息给当前发送者 */
declare function replyText(text: string): void;
/** 回复图片消息给当前发送者 */
declare function replyImage(path: string): void;
/** 回复文件消息给当前发送者 */
declare function replyFile(path: string, title?: string): void;
/** 回复语音消息给当前发送者 */
declare function replyVoice(path: string, durationMs: number): void;

// --- 钩子函数定义 ---

/**
 * onMessage 钩子可以返回的消息对象结构
 */
interface MessageResponse {
    /** * 消息类型 
     * @default "text"
     */
    type?: "text" | "image" | "file" | "voice";
    /** 文本消息的内容 (仅当 type 为 "text" 时有效) */
    content?: string;
    /** 文件、图片或语音的绝对路径 (仅当 type 为 "image"/"file"/"voice" 时有效) */
    path?: string;
    /** 文件标题/显示名称 (可选，仅用于 "file") */
    title?: string;
    /** 语音时长（毫秒，仅用于 "voice") */
    duration?: number;
}

/**
 * 消息钩子
 * @param talker 发送者的 ID (wxid)
 * @param content 消息内容
 * @param type 消息类型
 * @param isSend 是否为自己发出的消息
 * @returns 可以返回 string 直接发送文本，或返回 MessageResponse 对象发送复杂消息，返回 null/void 则不回复。
 */
declare function onMessage(
    talker: string, 
    content: string, 
    type: number, 
    isSend: boolean
): string | MessageResponse | null | void;

/**
 * 请求钩子
 * @param uri 请求的目标 URI
 * @param cgiId 请求的 CGI ID
 * @param json 数据体对象。你可以直接修改它并返回，或者返回一个全新的对象。
 * @returns 必须返回修改后的对象或字符串化的对象，否则修改不会生效。
 * 注意: 为了使 uri 与 cgiId 也能被修改，该方法的 API 将会在后续更新中改变。
 */
declare function onRequest(uri: string, cgiId: number, json: any): any;

/**
 * 响应钩子
 * @param uri 请求的目标 URI
 * @param cgiId 请求的 CGI ID
 * @param json 数据体对象。
 * @returns 必须返回修改后的对象或字符串化的对象，否则修改不会生效。
 * 注意: 为了使 uri 与 cgiId 也能被修改，该方法的 API 将会在后续更新中改变。
 */
declare function onResponse(uri: string, cgiId: number, json: any): any;
