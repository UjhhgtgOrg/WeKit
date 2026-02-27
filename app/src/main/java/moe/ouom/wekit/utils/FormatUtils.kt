package moe.ouom.wekit.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

fun formatBytesSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB", "PiB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()

    // Format to 2 decimal places
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return "%.2f %s".format(value, units[digitGroups])
}

fun formatEpoch(epochMs: Long, shouldPrependDate: Boolean = false): String {
    val formatter = DateTimeFormatter.ofPattern(if (shouldPrependDate) "yyyy:MM:dd HH:mm:ss" else "HH:mm:ss")
        .withZone(ZoneId.of("Asia/Shanghai"))
        .withLocale(Locale.CHINA)

    return formatter.format(Instant.ofEpochMilli(epochMs))
}