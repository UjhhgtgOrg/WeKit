# ==========================================================
# Global Attributes & Basics
# ==========================================================
# 保留注解、泛型签名、行号、源文件信息等
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,LineNumberTable,SourceFile,*Annotation*

# 保持所有包含原生方法的类
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留 Parcelable 的实现
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# 保持 Kotlin 函数类
-keep class kotlin.jvm.functions.** { *; }

# ==========================================================
# App & Module Specific
# ==========================================================
# 防止混淆 Xposed 模块代码
-keep class moe.ouom.wekit.** { *; }

# ==========================================================
# Jetpack Compose
# ==========================================================
# 核心运行时
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.runtime.internal.** { *; }
-keep class androidx.compose.runtime.saveable.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 手势与输入
-keep class androidx.compose.foundation.gestures.** { *; }
-keep class androidx.compose.ui.input.pointer.** { *; }
-keep class androidx.compose.ui.input.pointer.PointerInputScope { *; }
-keep class androidx.compose.ui.input.pointer.PointerInputChange { *; }
-keepclassmembers class androidx.compose.ui.input.pointer.PointerInputScope {
    *;
}

# Accompanist 库
-keep class com.google.accompanist.** { *; }

# ==========================================================
# Xposed & LSPosed
# ==========================================================
# 处理资源文件内容
-adaptresourcefilecontents META-INF/xposed/java_init.list

# 保持模块入口点
-keep,allowobfuscation,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>(...);
    public void onPackageLoaded(...);
    public void onSystemServerLoaded(...);
}

# 保持 LibXposed 注解
-keep,allowoptimization,allowobfuscation @io.github.libxposed.api.annotations.* class * {
    @io.github.libxposed.api.annotations.BeforeInvocation <methods>;
    @io.github.libxposed.api.annotations.AfterInvocation <methods>;
}

# 忽略 Xposed 库的警告
-dontwarn de.robv.android.xposed.**
-dontwarn io.github.libxposed.api.**

# 保持动画差值器
-keep class android.view.animation.PathInterpolator { *; }

# DexKit / Dalvik DX / ByteBuddy
-keep class com.android.dx.** { *; }
-keep class net.bytebuddy.** { *; }
-dontwarn com.sun.jna.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.lang.instrument.**

# ==========================================================
# Serialization
# ==========================================================
# --- Gson ---
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class com.google.gson.Gson { *; }
# 保留 @SerializedName 标注的字段
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# --- Kotlinx Serialization ---
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <methods>;
}

# 保持 Companion 对象的 serializer() 方法
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Protobuf ---
-keep class com.google.protobuf.**
-keepclassmembers public class * extends com.google.protobuf.MessageLite {*;}
-keepclassmembers public class * extends com.google.protobuf.MessageOrBuilder {*;}

# ==========================================================
# Network
# ==========================================================
# OkHttp & Okio
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ==========================================================
# Other
# ==========================================================

# 通用忽略警告
-dontwarn javax.**
-dontwarn java.awt.**
-dontwarn org.apache.bsf.*

# ==========================================================
# Side Effects
# ==========================================================
# 注意：只有在启用优化时，下面这些 assumenosideeffects 才会生效
# 如果保留了 -dontoptimize，这部分配置将被忽略

# 移除 Kotlin Intrinsics 检查
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

# 移除 Objects.requireNonNull 检查
-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}

# ==========================================================
# Build Behavior
# ==========================================================
-dontoptimize
-dontobfuscate