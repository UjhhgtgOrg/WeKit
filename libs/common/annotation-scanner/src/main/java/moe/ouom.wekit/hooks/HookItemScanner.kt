package moe.ouom.wekit.hooks

//import com.google.devtools.ksp.KspExperimental
//import com.google.devtools.ksp.getAnnotationsByType
//import com.google.devtools.ksp.processing.CodeGenerator
//import com.google.devtools.ksp.processing.Dependencies
//import com.google.devtools.ksp.processing.KSPLogger
//import com.google.devtools.ksp.processing.Resolver
//import com.google.devtools.ksp.processing.SymbolProcessor
//import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
//import com.google.devtools.ksp.processing.SymbolProcessorProvider
//import com.google.devtools.ksp.symbol.KSAnnotated
//import com.google.devtools.ksp.symbol.KSClassDeclaration
//import com.squareup.kotlinpoet.ClassName
//import com.squareup.kotlinpoet.CodeBlock
//import com.squareup.kotlinpoet.FileSpec
//import com.squareup.kotlinpoet.FunSpec
//import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
//import com.squareup.kotlinpoet.TypeSpec
//import com.squareup.kotlinpoet.ksp.toClassName
//import com.squareup.kotlinpoet.ksp.writeTo
//import moe.qq.hooks._core.annotation.HookItem

/**
 * KSP 处理器根据 @HookItem 注解扫描并生成代码
 */

//class HookItemProvider : SymbolProcessorProvider {
//    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
//        return HookItemScanner(environment.codeGenerator, environment.logger)
//    }
//}
//
//class HookItemScanner(
//    private val codeGenerator: CodeGenerator,
//    val logger: KSPLogger
//) : SymbolProcessor {
//
//    @OptIn(KspExperimental::class)
//    override fun process(resolver: Resolver): List<KSAnnotated> {
//        // 获取所有带有 @HookItem 注解的类
//        val symbols =
//            resolver.getSymbolsWithAnnotation("moe.qq.hooks._core.annotation.HookItem")
//                .filterIsInstance<KSClassDeclaration>()
//                .toList()
//
//        if (symbols.isEmpty()) return emptyList()
//
//        // 准备返回类型和基类
//        val returnType = ClassName("kotlin.collections", "List")
//        val genericsType = ClassName("moe.qq.hooks._base", "BaseHookItem")
//
//        // 创建方法构建器
//        val methodBuilder = FunSpec.builder("getAllHookItems")
//            .returns(returnType.parameterizedBy(genericsType)) // 泛型返回
//            .addAnnotation(JvmStatic::class) // 添加静态方法注解
//
//        // 构建方法体
//        methodBuilder.addCode(
//            CodeBlock.Builder().apply {
//                addStatement("val list = mutableListOf<BaseHookItem>()")
//
//                // 遍历所有被 @HookItem 注解的类
//                for (symbol in symbols) {
//                    val typeName = symbol.toClassName()
//                    val hookItem = symbol.getAnnotationsByType(HookItem::class).first()
//                    val itemName = hookItem.path
//                    val desc = hookItem.description
//
//                    // 为每个类生成对象实例并设置路径
//                    val valName = symbol.toClassName().simpleName
//                    addStatement("val %N = %T()", valName, typeName)
//                    addStatement("%N.setPath(%S)", valName, itemName)
//                    addStatement("%N.setDesc(%S)", valName, desc)
//                    addStatement("list.add(%N)", valName)
//                }
//                addStatement("return list")
//            }.build()
//        )
//
//        // 创建最终类
//        val classSpec = TypeSpec.objectBuilder("HookItemEntryList")
//            .addFunction(methodBuilder.build())
//            .build()
//
//        // 输出文件到指定目录
//        val dependencies = Dependencies(true, *symbols.map { it.containingFile!! }.toTypedArray())
//        FileSpec.builder("moe.qq.hook.gen", "HookItemEntryList")
//            .addType(classSpec)
//            .build()
//            .writeTo(codeGenerator, dependencies)
//
//        return emptyList()
//    }
//}