package net.ankio.qianji.hooks

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.qianji.BuildConfig
import net.ankio.qianji.R
import net.ankio.qianji.api.Hooker
import net.ankio.qianji.api.PartHooker
import net.ankio.qianji.databinding.ItemMenuBinding
import net.ankio.qianji.databinding.MenuListBinding
import net.ankio.qianji.server.model.AccountingConfig
import net.ankio.qianji.utils.HookUtils
import net.ankio.qianji.utils.SyncUtils
import net.ankio.qianji.utils.UserUtils

class SidePartHooker(hooker: Hooker) : PartHooker(hooker) {
    override val hookName: String = "钱迹左侧设置页"

    private val codeAuth = 52045001

    override fun onInit(
        classLoader: ClassLoader,
        context: Context,
    ) {
        hookMainActivity(classLoader, context)
    }

    private lateinit var syncUtils: SyncUtils
    private lateinit var binding: MenuListBinding

    private fun hookMainActivity(
        classLoader: ClassLoader,
        context: Context,
    ) {
        val clazz = classLoader.loadClass("com.mutangtech.qianji.ui.main.MainActivity")
        // 主Activity创建的时候，执行自动记账加载hook流程
        XposedHelpers.findAndHookMethod(
            clazz,
            "onCreate",
            android.os.Bundle::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    val activity = param!!.thisObject as Activity
                    /**
                     * activity as  ComponentActivity
                     */
                    hookMenu(activity, classLoader)
                    // 初始化同步工具（clazz加载）

                    syncUtils = SyncUtils.getInstance(activity, classLoader, hooker)
                    // 判断自动记账是否需要加载
                    val isAutoAccounting = HookUtils.getService().isConnected()
                    XposedBridge.log("$clazz onCreate  => isAutoAccounting ? $isAutoAccounting")
                }
            },
        )

        // 这是自动记账授权响应

        XposedHelpers.findAndHookMethod(
            "androidx.activity.ComponentActivity",
            classLoader,
            "onActivityResult",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Intent::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    val activity = param!!.thisObject as Activity
                    /**
                     * activity as  ComponentActivity
                     */
                    XposedBridge.log("onActivityResult")
                    val requestCode = param.args[0] as Int
                    val resultCode = param.args[1] as Int
                    val intent = param.args[2] as Intent

                    if (requestCode == codeAuth) {
                        if (resultCode != Activity.RESULT_OK) {
                            Toast.makeText(activity, "授权失败", Toast.LENGTH_SHORT).show()
                            return
                        }
                        val resultData = intent.getStringExtra("token")
                        log("onActivityResult => token: $resultData")
                        // 调用自动记账存储
                        HookUtils.getScope().launch {
                            HookUtils.getService().putToken(resultData!!)
                            HookUtils.getService().connect()
                            syncBillsFromAutoAccounting(activity)
                        }
                    }
                }
            },
        )
        // 这里是为了执行数据同步
        XposedHelpers.findAndHookMethod(
            clazz,
            "onResume",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    val activity = param!!.thisObject as Activity
                    // 如果自动记账功能打开，从自动记账同步账单
                    val isAutoAccounting = HookUtils.getService().isConnected()
                    XposedBridge.log("$clazz onResume => isAutoAccounting ? $isAutoAccounting")
                    if (isAutoAccounting) {
                        HookUtils.getScope().launch {
                            syncBillsFromAutoAccounting(activity)
                        }
                    }
                }
            },
        )
    }

    private fun hookMenu(
        activity: Activity,
        classLoader: ClassLoader?,
    ) {
        var hooked = false
        val clazz = classLoader!!.loadClass("com.mutangtech.qianji.ui.maindrawer.MainDrawerLayout")
        XposedHelpers.findAndHookMethod(
            clazz,
            "refreshAccount",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    // 只hook一次
                    val obj = param!!.thisObject as FrameLayout

                    setVipName(obj, classLoader)

                    if (hooked) return
                    hooked = true

                    // 调用 findViewById 并转换为 TextView
                    val linearLayout =
                        getViewById(obj, classLoader, "main_drawer_content_layout") as LinearLayout
                    runCatching {
                        HookUtils.addAutoContext(activity)
                        // 找到了obj里面的name字段
                        addSettingMenu(linearLayout, activity, classLoader)
                    }.onFailure {
                        XposedBridge.log(it)
                    }
                }
            },
        )
    }

    private fun onGetAutoApplication(context: Activity) {
        HookUtils.toastError("未找到自动记账，请从Github下载自动记账App")
        // 跳转自动记账下载页面：https://github.com/AutoAccountingOrg/AutoAccounting/
        val url = "https://github.com/AutoAccountingOrg/AutoAccounting/"
        val intentAuto = Intent(Intent.ACTION_VIEW)
        intentAuto.data = Uri.parse(url)
        try {
            context.startActivity(intentAuto)
        } catch (e: ActivityNotFoundException) {
            HookUtils.toastError("没有安装任何支持的浏览器客户端")
        }
    }

    private suspend fun syncBillsFromAutoAccounting(activity: Activity) =
        withContext(Dispatchers.IO) {

            val last = HookUtils.readData("lastSyncTime").toLongOrNull() ?: 0
            val now = System.currentTimeMillis()
            if (now - last < 1000 * 60) {
                HookUtils.log("距离上次同步时间不足1分钟")
                return@withContext
            }
            HookUtils.writeData("lastSyncTime", now.toString())
            if (!UserUtils.isLogin(hooker)) {
                HookUtils.toastError("未登录用户不支持同步数据")
                return@withContext
            }
            HookUtils.getScope().launch {
                runCatching {
                    log("同步账本")
                    syncUtils.books()

                    log("同步资产")
                    syncUtils.assets()

                    log("同步账单")
                    syncUtils.billsFromQianJi()

                    log("从自动记账同步账单")
                    syncUtils.billsFromAuto()
                }.onFailure {
                    XposedBridge.log(it)
                }
            }
        }

    /**
     * 设置Vip名称
     */
    private fun setVipName(
        obj: FrameLayout,
        classLoader: ClassLoader?,
    ) {
        val vipName = HookUtils.readData("vipName")
        if (vipName !== "") {
            // 调用 findViewById 并转换为 TextView
            val textView = getViewById(obj, classLoader!!, "settings_vip_badge") as TextView
            textView.visibility = View.VISIBLE
            textView.text = vipName
        }
    }

    private lateinit var rClass: Class<*>

    private fun getViewById(
        obj: FrameLayout,
        classLoader: ClassLoader,
        id: String,
    ): View {
        if (!::rClass.isInitialized) {
            rClass = Class.forName("com.mutangtech.qianji.R\$id", true, classLoader)
        }
        val resourceId = rClass.getField(id).getInt(null)
        // 调用 findViewById 并转换为 TextView
        return XposedHelpers.callMethod(
            obj,
            "findViewById",
            resourceId,
        ) as View
    }

    private fun addSettingMenu(
        linearLayout: LinearLayout,
        context: Activity,
        classLoader: ClassLoader?,
    ) {
        val isDarkMode: Boolean = isDarkMode(context)
        val mainColor = if (isDarkMode) -0x2c2c2d else -0xcacacb
        val subColor = if (isDarkMode) -0x9a9a9b else -0x666667
        val backgroundColor = if (isDarkMode) -0xd1d1d2 else -0x1

        val itemMenuBinding = ItemMenuBinding.inflate(LayoutInflater.from(context))
        itemMenuBinding.root.setBackgroundColor(Color.parseColor(if (isDarkMode) "#191919" else "#ffffff"))

        itemMenuBinding.title.text = context.getString(R.string.app_name)
        itemMenuBinding.title.setTextColor(Color.parseColor(if (isDarkMode) "#b6b6b6" else "#2c2c2c"))

        itemMenuBinding.version.text = BuildConfig.VERSION_NAME
        itemMenuBinding.version.setTextColor(subColor)
        itemMenuBinding.root.setOnClickListener {
            binding = MenuListBinding.inflate(LayoutInflater.from(context))
            // 弹出自定义布局
            val menuListView = binding.root
            menuListView.setBackgroundColor(backgroundColor)
            // 弹窗AlertDialog
            // 遍历binding的所有属性，所有父类型是view的设置背景色和前景色
            binding::class.java.declaredFields.forEach {
                if (View::class.java.isAssignableFrom(it.type)) {
                    it.isAccessible = true
                    val bindingView = it.get(binding) as View
                    if (TextView::class.java.isAssignableFrom(it.type)) {
                        (bindingView as TextView).setTextColor(mainColor)
                    } else if (Switch::class.java.isAssignableFrom(it.type)) {
                        (bindingView as Switch).setTextColor(mainColor)
                    } else if (EditText::class.java.isAssignableFrom(it.type)) {
                        (bindingView as EditText).setTextColor(mainColor)
                    } else if (Button::class.java.isAssignableFrom(it.type)) {
                        (bindingView as Button).setTextColor(mainColor)
                    }
                    //   }
                }
            }

            binding.autoAccounting.isChecked = HookUtils.getService().isConnected()

            binding.autoAccounting.setOnCheckedChangeListener { buttonView, isChecked ->
                if (!isChecked) {
                    HookUtils.getService().putToken("")
                    return@setOnCheckedChangeListener
                }
                // binding.autoAccounting.isChecked = false
                val intent = Intent("net.ankio.auto.ACTION_REQUEST_AUTHORIZATION")
                // 设置包名，用于自动记账对目标app进行检查
                intent.putExtra("packageName", hooker.packPageName)
                try {
                    context.startActivityForResult(intent, codeAuth)
                } catch (e: ActivityNotFoundException) {
                    onGetAutoApplication(context)
                    // 没有自动记账，需要引导用户下载自动记账App
                }
            }

            binding.editTextText.setText(HookUtils.readData("vipName"))
            binding.editTextText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    HookUtils.writeData("vipName", v.text.toString())
                    setVipName(linearLayout.parent as FrameLayout, classLoader)
                }
                false
            }

            val config = AccountingConfig.getConfig()
            config::class.java.declaredFields.forEach {
                it.isAccessible = true
                if (it.type !== Boolean::class.java) {
                    return@forEach
                }
                val name = it.name
                val value = it.get(config) as Boolean
                val switch = binding::class.java.getDeclaredField(name).get(binding) as Switch
                switch.isChecked = value
                switch.setOnCheckedChangeListener { buttonView, isChecked ->
                    it.set(config, isChecked)
                    HookUtils.getScope().launch {
                        AccountingConfig.saveAndSync(config)
                    }
                }
            }

            // 创建AlertDialog并设置自定义视图
            val dialog: AlertDialog =
                AlertDialog.Builder(context)
                    .setIcon(R.mipmap.ic_launcher)
                    .setView(menuListView)
                    .create()
// 显示弹窗
            dialog.show()
        }
        linearLayout.addView(itemMenuBinding.root)
    }

    private fun isDarkMode(context: Context): Boolean {
        return Configuration.UI_MODE_NIGHT_YES == (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
    }
}
