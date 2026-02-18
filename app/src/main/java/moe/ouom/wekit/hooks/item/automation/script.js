/// <reference path="./globals.d.ts" />

function getCleanContent(content) {
    // Remove "wxid_xxx:\n" prefix in group chats
    var match = content.match(/^wxid_[^:]+:\n(.*)$/s);
    if (match) {
        return match[1];
    }
    return content;
}

function commandWeather(content) {
    log.i("fetching weather...");

    var cityName = content.substring(8).trim();

    // Default to Shanghai if no city specified
    if (cityName === "") {
        cityName = "上海";
    }

    log.i("querying weather for:", cityName);

    // City code mapping (you can expand this)
    var cityCodeMap = {
        北京: "101010100",
        上海: "101020100",
        广州: "101280101",
        深圳: "101280601",
        杭州: "101210101",
        成都: "101270101",
        武汉: "101200101",
        西安: "101110101",
        重庆: "101040100",
        天津: "101030100",
        南京: "101190101",
        苏州: "101190401",
        郑州: "101180101",
        长沙: "101250101",
        沈阳: "101070101",
        青岛: "101120201",
        厦门: "101230201",
        大连: "101070201",
        济南: "101120101",
        哈尔滨: "101050101"
    };

    var cityCode = cityCodeMap[cityName];

    if (!cityCode) {
        log.w("city not found in map:", cityName);
        return (
            "抱歉，暂不支持查询该城市天气。\n支持的城市：" +
            Object.keys(cityCodeMap).join("、")
        );
    }

    // Make request to Xiaomi Weather API
    var response = http.get(
        "https://weatherapi.market.xiaomi.com/wtr-v3/weather/all",
        {
            latitude: "0",
            longitude: "0",
            locationKey: "weathercn:" + cityCode,
            sign: "zUFJoAR2ZVrDy1vF3D07",
            isGlobal: "false",
            locale: "zh_cn",
            days: "1",
            appKey: "weather20151024"
        }
    );

    log.i("api response status:", response.status);

    if (!response.ok) {
        log.e("weather api request failed");
        log.e("status:", response.status);
        log.e("error:", response.error);
        return "天气查询失败，请稍后重试";
    }

    if (!response.json) {
        log.e("response is not json");
        log.e("body:", response.body);
        return "天气数据解析失败";
    }

    var data = response.json;
    log.d("full response:", JSON.stringify(data));

    // Check if current weather data exists
    if (!data.current) {
        log.e("no current weather data in response");
        return "未获取到天气数据";
    }

    var current = data.current;

    // Weather code to description mapping
    var weatherMap = {
        0: "晴",
        1: "多云",
        2: "阴",
        3: "阵雨",
        4: "雷阵雨",
        5: "雷阵雨伴有冰雹",
        6: "雨夹雪",
        7: "小雨",
        8: "中雨",
        9: "大雨",
        10: "暴雨",
        11: "大暴雨",
        12: "特大暴雨",
        13: "阵雪",
        14: "小雪",
        15: "中雪",
        16: "大雪",
        17: "暴雪",
        18: "雾",
        19: "冻雨",
        20: "沙尘暴",
        21: "小到中雨",
        22: "中到大雨",
        23: "大到暴雨",
        24: "暴雨到大暴雨",
        25: "大暴雨到特大暴雨",
        26: "小到中雪",
        27: "中到大雪",
        28: "大到暴雪",
        29: "浮尘",
        30: "扬沙",
        31: "强沙尘暴",
        32: "霾",
        53: "霾"
    };

    var weatherDesc = weatherMap[current.weather] || "未知";
    var temperature = current.temperature.value + current.temperature.unit;
    var feelsLike = current.feelsLike.value + current.feelsLike.unit;
    var humidity = current.humidity.value + current.humidity.unit;
    var pressure = current.pressure.value + current.pressure.unit;
    var windSpeed = current.wind.speed.value + current.wind.speed.unit;
    var windDir = current.wind.direction.value + current.wind.direction.unit;
    var uvIndex = current.uvIndex;

    log.i("weather parsed successfully for", cityName);

    // Format response message
    var message =
        "📍 " +
        cityName +
        " 天气\n" +
        "━━━━━━━━━━━━\n" +
        "🌡️ 温度：" +
        temperature +
        "\n" +
        "🤚 体感：" +
        feelsLike +
        "\n" +
        "☁️ 天气：" +
        weatherDesc +
        "\n" +
        "💧 湿度：" +
        humidity +
        "\n" +
        "🎐 气压：" +
        pressure +
        "\n" +
        "💨 风速：" +
        windSpeed +
        "\n" +
        "🧭 风向：" +
        windDir +
        "\n" +
        "☀️ 紫外线：" +
        uvIndex +
        "\n" +
        "━━━━━━━━━━━━\n" +
        "⏰ 更新时间：" +
        current.pubTime;

    return message;
}

function commandRandomPic(content) {
    log.i("fetching random picture...");
    var sourceName = content.substring(11).trim();
    if (sourceName === "") {
        sourceName = "alcy";
    }

    log.d("sourceName=" + sourceName);

    if (sourceName === "alcy") {
        log.i("fetching random picture from Alcy...");

        var response = http.get("https://t.alcy.cc/ysz", {
            json: "",
            quantity: "1"
        });

        log.i("api response status:", response.status);

        if (!response.ok) {
            log.e("pic api request failed");
            log.e("status:", response.status);
            log.e("error:", response.error);
            replyText("图片获取失败，请稍后重试");
        }

        var url = response.body.trim();
        var result = http.download(url);

        if (!result.ok) {
            log.e("failed to download picture");
            replyText("图片下载失败，请稍后重试");
        }

        replyImage(result.path);
    } else {
        replyText("暂不支持当前来源，请等待开发者实现喵");
    }
}

function commandHitokoto() {
    log.i("fetching sentence from hitokoto v1 api...");
    var response = http.get("https://v1.hitokoto.cn/");

    if (!response.ok) {
        log.e("hitokoto api request failed");
        log.e("status:", response.status);
        log.e("error:", response.error);
        replyText("一言获取失败，请稍后重试");
    }

    if (!response.json) {
        log.e("response is not json");
        log.e("body:", response.body);
        return "一言数据解析失败";
    }

    var data = response.json;
    log.d("full response:", JSON.stringify(data));

    // Format response message
    if (data.from_who) {
        var message =
            "『" +
            data.hitokoto +
            "』\n" +
            "        —— " +
            data.from_who +
            "「" +
            data.from +
            "」";
    } else {
        var message =
            "『" +
            data.hitokoto +
            "』\n" +
            "        —— " +
            "「" +
            data.from +
            "」";
    }

    return message;
}

function commandDebugMsg(talker, content) {
    var key = talker + "_debug_msg_enabled";
    if (!cache.hasKey(key)) {
        cache.set(key, true);
        return "已启用消息调试模式, 将会输出下一条消息的原始对象.";
    } else {
        var val = cache.get(key);
        cache.set(key, !val);
        if (val) {
            return "已禁用消息调试模式";
        } else {
            return "已启用消息调试模式, 将会输出下一条消息的原始对象.";
        }
    }
}

function commandHelp(content) {
    var cmdName = content.substring(5).trim();

    if (cmdName === "help") {
        return (
            "/help\n" +
            "功能: 输出命令帮助.\n" +
            "用法: /help <命令>\n" +
            "参数:\n" +
            "1. 命令: 可选, 若不指定此参数则输出全部可用命令列表."
        );
    }

    if (cmdName === "changelog") {
        return (
            "/changelog\n" +
            "功能: 输出更新内容.\n" +
            "用法: /changelog\n" +
            "参数:\n" +
            "无"
        );
    }

    if (cmdName === "weather") {
        return (
            "/weather\n" +
            "功能: 输出城市当前天气.\n" +
            "用法: /weather <城市>\n" +
            "参数:\n" +
            "1. 城市: 可选, 默认为'上海'."
        );
    }

    if (cmdName === "random-pic") {
        return (
            "/random-pic\n" +
            "功能: 获取随机二次元图片.\n" +
            "用法: /random-pic <来源>\n" +
            "参数:\n" +
            "1. 来源: 可选, 默认为 'alcy', 可选项: alcy,yande.re,konachan,zerochan,danbooru,gelbooru,waifu.im,wallhaven\n" +
            "(P.S. 除了 alcy 以外我还全都没实现, 输了没用)"
        );
    }

    if (cmdName === "hitokoto") {
        return (
            "/hitokoto\n" +
            "功能: 输出「一言」.\n" +
            "用法: /hitokoto\n" +
            "参数:\n" +
            "无"
        );
    }

    if (cmdName === "debug-msg") {
        return (
            "/debug-msg\n" +
            "功能: 为当前聊天启用或禁用消息调试模式. 启用该模式将输出下一条消息的原始对象.\n" +
            "用法: /debug-msg\n" +
            "参数:\n" +
            "无"
        );
    }

    return (
        "可用命令 (可使用 /help <命令> 查看详细帮助):\n" +
        "/help\n" +
        "/changelog\n" +
        "/weather\n" +
        "/random-pic\n" +
        "/hitokoto\n" +
        "/debug-msg"
    );
}

function commmandChangelog() {
    return (
        "更新内容:\n" +
        "2026.02.17 - 模块添加 '自动回复' 功能\n" +
        "2026.02.18 - 功能重构为 '自动化', 与原 '脚本管理' 合并, 移除了除 JavaScript 以外的消息匹配方式\n" +
        "             添加命令 help, changelog, weather, random-pic, hitokoto, debug-msg"
    );
}

function onMessage(talker, content, type, isSend) {
    log.i("onMessage() triggered");

    content = getCleanContent(content);

    if (content.startsWith("/debug-msg")) {
        return commandDebugMsg(talker, content);
    }

    var debugMsgKey = talker + "_debug_msg_enabled";
    if (cache.getOrDefault(debugMsgKey, false)) {
        cache.set(debugMsgKey, false);

        var message =
            "消息调试：\n" +
            "talker=" +
            talker +
            "\n" +
            "content=" +
            content +
            "\n" +
            "type=" +
            type +
            "\n" +
            "isSend=" +
            isSend +
            "\n";

        return message;
    }

    if (content.startsWith("/help")) {
        return commandHelp(content);
    }

    if (content.startsWith("/changelog")) {
        return commmandChangelog();
    }

    if (content.startsWith("/weather")) {
        return commandWeather(content);
    }

    if (content.startsWith("/random-pic")) {
        commandRandomPic(content);
        return null;
    }

    if (content.startsWith("/hitokoto")) {
        return commandHitokoto();
    }

    if (
        content.startsWith("/time") ||
        content.startsWith("/kill") ||
        content.startsWith("/op") ||
        content.startsWith("/deop") ||
        content.startsWith("/ban") ||
        content.startsWith("/pardon") ||
        content.startsWith("/time")
    ) {
        return "bro这不是mc你发mc指令干甚么[骷髅]";
    }

    if (content.startsWith("/")) {
        return "暂不支持该命令，请等待开发者实现喵";
    }

    return null;
}
