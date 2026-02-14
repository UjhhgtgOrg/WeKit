package moe.ouom.wekit.ui.creator.dialog

import android.content.Context
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import moe.ouom.wekit.ui.CommonContextWrapper
import moe.ouom.wekit.util.common.ModuleRes
import moe.ouom.wekit.util.common.Toasts.showToast
import moe.ouom.wekit.util.log.WeLogger
import moe.ouom.wekit.util.script.ScriptEvalManager
import moe.ouom.wekit.util.script.ScriptFileManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * 脚本管理器对话框
 */
class ScriptManagerDialog(
    context: Context,
    private val scriptManager: ScriptFileManager,
    private val scriptEvalManager: ScriptEvalManager
) : BaseSettingsDialog(context, "脚本管理器") {

    private val scripts = mutableListOf<ScriptFileManager.ScriptConfig>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun initList() {
        contentContainer.removeAllViews()
        loadScripts()
        renderScriptList()
    }

    private fun loadScripts() {
        scripts.clear()
        scripts.addAll(scriptManager.getAllScripts())
    }

    private fun renderScriptList() {
        if (scripts.isEmpty()) {
            renderEmptyView()
            renderActionButtons()
            return
        }

        scripts.sortBy { it.order }
        scripts.forEachIndexed { index, script ->
            renderScriptItem(index, script)
        }

        renderActionButtons()
    }

    private fun renderEmptyView() {
        val emptyView = TextView(context).apply {
            text = "暂无脚本，点击下方按钮添加"
            gravity = Gravity.CENTER
            textSize = 16f
            setPadding(32, 64, 32, 64)
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }
        contentContainer.addView(emptyView)
    }

    private fun renderScriptItem(index: Int, script: ScriptFileManager.ScriptConfig) {
        // 使用 inflateItem 方式创建视图
        val view = inflateItem("script_item", contentContainer) ?: return

        // 获取视图组件
        val tvTitle = view.findViewById<TextView>(ModuleRes.getId("title", "id"))
        val tvUuid = view.findViewById<TextView>(ModuleRes.getId("uuid", "id"))
        val tvTime = view.findViewById<TextView>(ModuleRes.getId("time", "id"))
        val tvDesc = view.findViewById<TextView>(ModuleRes.getId("description", "id"))
        val cbEnabled = view.findViewById<MaterialCheckBox>(ModuleRes.getId("enabled_checkbox", "id"))
        val btnCopy = view.findViewById<MaterialButton>(ModuleRes.getId("copy_button", "id"))
        val btnActions = view.findViewById<MaterialButton>(ModuleRes.getId("actions_button", "id"))

        // 设置标题
        tvTitle.text = "${index + 1}. ${script.name}"

        // 设置UUID
        tvUuid.text = "ID: ${script.id}..."

        // 设置时间信息
        val createdTime = dateFormat.format(Date(script.createdTime))
        val modifiedTime = dateFormat.format(Date(script.modifiedTime))
        tvTime.text = "创建: $createdTime | 修改: $modifiedTime"

        // 设置描述
        if (script.description.isNotEmpty()) {
            tvDesc.text = "描述: ${script.description}"
            tvDesc.visibility = View.VISIBLE
        } else {
            tvDesc.visibility = View.GONE
        }

        // 设置启用状态
        cbEnabled.isChecked = script.enabled

        // 设置启用状态监听器
        cbEnabled.setOnCheckedChangeListener { _, isChecked ->
            script.enabled = isChecked
            scriptManager.saveScript(script)
            showToast(context, if (isChecked) "已启用" else "已禁用")
        }

        // 设置复制按钮监听器
        btnCopy.setOnClickListener {
            copyFullScriptInfo(script)
        }

        // 设置操作按钮监听器
        btnActions.setOnClickListener {
            showActionMenu(index, script)
        }

        // 设置整个视图的点击监听器，进入脚本编辑
        view.setOnClickListener {
            showEditDialog(index)
        }

        contentContainer.addView(view)

        // 添加分隔线（如果不是最后一个）
        if (index < scripts.size - 1) {
            val divider = TextView(context).apply {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                ).apply {
                    setMargins(32, 16, 32, 16)
                }
            }
            contentContainer.addView(divider)
        }
    }

    /**
     * 显示操作菜单
     */
    private fun showActionMenu(index: Int, script: ScriptFileManager.ScriptConfig) {
        val wrappedContext = CommonContextWrapper.createAppCompatContext(context)
        val options = mutableListOf<String>()
        val isTop = index == 0
        val isBottom = index == scripts.size - 1

        options.add("上移")
        options.add("下移")
        options.add("编辑")
        options.add("删除")
        options.add("测试")

        MaterialDialog(wrappedContext)
            .title(text = "脚本操作")
            .listItems(items = options) { dialog, optionIndex, _ ->
                when (optionIndex) {
                    0 -> {
                        // 上移
                        if (isTop) {
                            showToast(context, "已在顶部，无法上移")
                            return@listItems
                        }
                        moveScriptUp(index)
                    }

                    1 -> {
                        // 下移
                        if (isBottom) {
                            showToast(context, "已在底部，无法下移")
                            return@listItems
                        }
                        moveScriptDown(index)
                    }

                    2 -> showEditDialog(index)
                    3 -> confirmDeleteScript(index)
                    4 -> showTestDialog(index)
                }

                dialog.dismiss()
            }
            .negativeButton(text = "取消")
            .show()
    }

    /**
     * 复制完整的脚本信息
     */
    private fun copyFullScriptInfo(script: ScriptFileManager.ScriptConfig) {
        val contentEncoded = Base64.encodeToString(script.content.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        val jsonObject = JSONObject().apply {
            put("name", script.name)
            put("id", script.id)
            put("description", script.description)
            put("content", contentEncoded)
        }

        copyToClipboard("脚本JSON", jsonObject.toString(2))
        showToast(context, "已复制脚本JSON格式")
    }

    private fun renderActionButtons() {
        val buttonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(32, 24, 32, 32)
        }

        val btnAdd = MaterialButton(context).apply {
            text = "添加脚本"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }
            setOnClickListener {
                showAddDialog()
            }
        }

        val btnImport = MaterialButton(context).apply {
            text = "导入"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                showImportOptions()
            }
        }

        buttonLayout.addView(btnAdd)
        buttonLayout.addView(btnImport)
        contentContainer.addView(buttonLayout)
    }

    private fun showAddDialog() {
        showEditDialog(-1)
    }

    private fun showEditDialog(index: Int) {
        val isNew = index == -1
        val script = if (isNew) {
            val newId = UUID.randomUUID().toString()
            ScriptFileManager.ScriptConfig(
                id = newId,
                name = "",
                content = "",
                order = scripts.size
            )
        } else {
            scripts.getOrNull(index) ?: return
        }

        val wrappedContext = CommonContextWrapper.createAppCompatContext(context)

        // 使用 ModuleRes 加载布局
        val dialogView = ModuleRes.inflate("dialog_script_edit", null)

        // 获取视图组件
        val tvUuid = dialogView.findViewById<TextView>(ModuleRes.getId("tv_uuid", "id"))
        val etName = dialogView.findViewById<TextView>(ModuleRes.getId("et_name", "id"))
        val etDesc = dialogView.findViewById<TextView>(ModuleRes.getId("et_desc", "id"))
        val etContent = dialogView.findViewById<TextView>(ModuleRes.getId("et_content", "id"))

        // 设置UUID
        tvUuid.text = "脚本ID: ${script.id}"

        // 设置现有值
        etName.text = script.name
        etDesc.text = script.description ?: ""
        etContent.text = script.content

        MaterialDialog(wrappedContext)
            .customView(view = dialogView)
            .title(text = if (isNew) "添加脚本" else "编辑脚本")
            .positiveButton(text = "保存") {
                val name = etName.text.toString().trim()
                val description = etDesc.text.toString().trim()
                val content = etContent.text.toString().trim()

                if (name.isEmpty()) {
                    showToast(context, "请输入脚本名称")
                    return@positiveButton
                }

                if (content.isEmpty()) {
                    showToast(context, "请输入脚本内容")
                    return@positiveButton
                }

                saveScript(script, name, description, content, isNew)
            }
            .neutralButton(text = "复制脚本内容") {
                val content = etContent.text.toString().trim()
                if (content.isNotEmpty()) {
                    copyToClipboard("脚本内容", content)
                    showToast(context, "已复制脚本内容")
                } else {
                    showToast(context, "脚本内容为空")
                }
            }
            .negativeButton(text = "取消")
            .show()
    }

    /**
     * 显示测试对话框
     */
    private fun showTestDialog(index: Int) {
        val script = scripts.getOrNull(index) ?: return
        val wrappedContext = CommonContextWrapper.createAppCompatContext(context)

        // 使用 ModuleRes 加载布局
        val dialogView = ModuleRes.inflate("dialog_test_script", null) ?: return

        // 获取视图组件
        val tvScriptName = dialogView.findViewById<TextView>(ModuleRes.getId("tv_script_name", "id"))
        val etInput = dialogView.findViewById<TextView>(ModuleRes.getId("et_input", "id"))

        // 设置脚本名称
        tvScriptName.text = "测试脚本: ${script.name}"

        MaterialDialog(wrappedContext)
            .customView(view = dialogView)
            .title(text = "测试脚本执行")
            .positiveButton(text = "执行") {
                val jsCode = etInput.text.toString().trim()
                if (jsCode.isNotEmpty()) {
                    executeTestScript(script, jsCode)
                } else {
                    showToast(context, "请输入要执行的JavaScript代码")
                }
            }
            .negativeButton(text = "取消")
            .show()
    }

    /**
     * 执行测试脚本
     */
    private fun executeTestScript(script: ScriptFileManager.ScriptConfig, jsCode: String) {
        val wrappedContext = CommonContextWrapper.createAppCompatContext(context)

        val result = scriptEvalManager.testExecuteCode(script.content, jsCode, "${script.name}+测试脚本")
        MaterialDialog(wrappedContext)
            .title(text = "执行结果")
            .message(text = result ?: "执行失败或无返回值")
            .positiveButton(text = "复制结果") {
                copyToClipboard("执行结果", result ?: "执行失败")
            }
            .negativeButton(text = "关闭")
            .show()
    }

    private fun saveScript(
        script: ScriptFileManager.ScriptConfig,
        name: String,
        description: String,
        content: String,
        isNew: Boolean
    ) {
        // 检测脚本是否正常
        val testResult = scriptEvalManager.testScriptMethods(content)
        if (!testResult.isPassed()) {
            val errorMsg = testResult.getSummary()
            showToast(context, "脚本验证失败，请修正后保存")

            // 显示详细错误信息
            val wrappedContext = CommonContextWrapper.createAppCompatContext(context)
            MaterialDialog(wrappedContext)
                .title(text = "脚本验证失败")
                .message(text = errorMsg)
                .positiveButton(text = "返回编辑") {
                    showEditDialog(if (isNew) -1 else scripts.indexOf(script))
                }
                .negativeButton(text = "取消")
                .show()
            return
        }

        script.name = name
        script.description = description
        script.content = content
        script.modifiedTime = System.currentTimeMillis()

        if (isNew) {
            script.createdTime = System.currentTimeMillis()
            scripts.add(script)
            scriptManager.saveScript(script)
            showToast(context, "脚本已添加")
        } else {
            scriptManager.saveScript(script)
            showToast(context, "脚本已更新")
        }

        contentContainer.removeAllViews()
        renderScriptList()
    }

    private fun showImportOptions() {
        val wrappedContext = CommonContextWrapper.createAppCompatContext(context)
        val options = listOf("从剪贴板导入", "导入示例")

        MaterialDialog(wrappedContext)
            .title(text = "导入脚本")
            .listItems(items = options) { dialog, optionIndex, _ ->
                when (optionIndex) {
                    0 -> importFromClipboard()
                    1 -> importExample()
                }
                dialog.dismiss()
            }
            .negativeButton(text = "取消")
            .show()
    }

    private fun importFromClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clipData = clipboard?.primaryClip

            if (clipData != null && clipData.itemCount > 0) {
                val clipItem = clipData.getItemAt(0)
                val text = clipItem.text?.toString()

                if (!text.isNullOrBlank()) {
                    // 尝试解析为JSON格式
                    val jsonObject = JSONObject(text)

                    // 验证必需字段
                    if (jsonObject.has("name") && jsonObject.has("id") && jsonObject.has("description") && jsonObject.has(
                            "content"
                        )
                    ) {
                        val newName = jsonObject.optString("name", "从剪贴板导入")
                        val newId = jsonObject.optString("id", UUID.randomUUID().toString())
                        val newDescription = jsonObject.optString("description", "")
                        val contentEncoded = jsonObject.optString("content", "")

                        // 解码内容
                        val contentBytes = Base64.decode(contentEncoded, Base64.NO_WRAP)
                        val newContent = String(contentBytes, Charsets.UTF_8)

                        // 检查是否已存在相同ID的脚本
                        val existingScript = scripts.find { it.id == newId }
                        if (existingScript != null) {
                            // 覆盖现有脚本
                            existingScript.name = newName
                            existingScript.content = newContent
                            existingScript.description = newDescription
                            existingScript.modifiedTime = System.currentTimeMillis()

                            scriptManager.saveScript(existingScript)
                            showToast(context, "已覆盖相同ID的脚本")

                            contentContainer.removeAllViews()
                            renderScriptList()
                            return
                        }

                        val newScript = ScriptFileManager.ScriptConfig(
                            id = newId,
                            name = newName,
                            content = newContent,
                            description = newDescription,
                            order = scripts.size
                        )

                        scriptManager.saveScript(newScript)
                        scripts.add(newScript)

                        contentContainer.removeAllViews()
                        renderScriptList()
                        showToast(context, "已从剪贴板导入脚本")
                    } else {
                        showToast(context, "剪贴板内容格式不正确，缺少必要字段(name, id, description, content)")
                    }
                } else {
                    showToast(context, "剪贴板内容为空")
                }
            } else {
                showToast(context, "剪贴板无内容")
            }
        } catch (e: Exception) {
            WeLogger.e("从剪贴板导入失败: ${e.message}")
            showToast(context, "导入失败，请确保剪贴板内容为有效的JSON格式")
        }
    }

    private fun importExample() {
        val exampleScripts = listOf(
            ScriptFileManager.ScriptConfig(
                id = UUID.randomUUID().toString(),
                name = "打印示例",
                content = """
                        // 这是一个简单的示例脚本 将打印参数
                        function onRequest(data) {
                            const {uri,cgiId,jsonData} = data;
                            wekit.log('拦截请求:', uri, cgiId, JSON.stringify(jsonData));
                            return jsonData;
                        }
                        
                        function onResponse(data) {
                            const {uri,cgiId,jsonData} = data;
                            wekit.log('拦截响应:', uri, cgiId, JSON.stringify(jsonData));
                            return jsonData;
                        }
                    """.trimIndent(),
                description = "最简单的示例脚本，包含onRequest和onResponse方法",
                order = scripts.size
            )
        )

        exampleScripts.forEach { script ->
            scriptManager.saveScript(script)
            scripts.add(script)
        }

        contentContainer.removeAllViews()
        renderScriptList()
        showToast(context, "已导入示例脚本")
    }

    private fun confirmDeleteScript(index: Int) {
        val script = scripts.getOrNull(index) ?: return
        val wrappedContext = CommonContextWrapper.createAppCompatContext(context)

        MaterialDialog(wrappedContext)
            .title(text = "确认删除")
            .message(text = "确定要删除脚本「${script.name}」吗？\nID: ${script.id}")
            .positiveButton(text = "删除") {
                deleteScript(index)
            }
            .negativeButton(text = "取消")
            .show()
    }

    private fun deleteScript(index: Int) {
        val script = scripts.getOrNull(index) ?: return

        if (scriptManager.deleteScript(script.id)) {
            scripts.removeAt(index)

            scripts.forEachIndexed { i, s ->
                s.order = i
                scriptManager.saveScript(s)
            }

            contentContainer.removeAllViews()
            renderScriptList()
            showToast(context, "脚本已删除")
        } else {
            showToast(context, "删除失败")
        }
    }

    private fun moveScriptUp(index: Int) {
        if (index > 0) {
            val temp = scripts[index]
            scripts[index] = scripts[index - 1]
            scripts[index - 1] = temp

            scripts.forEachIndexed { i, s ->
                s.order = i
                scriptManager.saveScript(s)
            }

            contentContainer.removeAllViews()
            renderScriptList()
        }
    }

    private fun moveScriptDown(index: Int) {
        if (index < scripts.size - 1) {
            val temp = scripts[index]
            scripts[index] = scripts[index + 1]
            scripts[index + 1] = temp

            scripts.forEachIndexed { i, s ->
                s.order = i
                scriptManager.saveScript(s)
            }

            contentContainer.removeAllViews()
            renderScriptList()
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
        } catch (e: Exception) {
            WeLogger.e("复制失败: ${e.message}")
        }
    }

}