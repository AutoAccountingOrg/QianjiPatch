package net.ankio.qianji.hooks


import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.sdk.AutoAccounting
import net.ankio.auto.sdk.exception.AutoAccountingException
import net.ankio.dex.Dex
import net.ankio.dex.model.ClazzField
import net.ankio.qianji.BuildConfig
import net.ankio.qianji.HookMainApp
import net.ankio.qianji.R
import net.ankio.qianji.api.Hooker
import net.ankio.qianji.api.PartHooker


class SidePartHooker(hooker: Hooker) :PartHooker(hooker) {
    override val hookName: String = "钱迹左侧设置页"

    private val codeAuth = 52045001

    override fun onInit(classLoader: ClassLoader?, context: Context?) {
        if(classLoader == null) return

        val clazz = classLoader.loadClass("com.mutangtech.qianji.ui.main.MainActivity")
        XposedHelpers.findAndHookMethod(clazz, "onCreate", android.os.Bundle::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                super.beforeHookedMethod(param)

            }
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val activity = param!!.thisObject as Activity
                /**
                 * activity as  ComponentActivity
                 */
                hookMenu(activity,classLoader)
                hooker.syncUtils.init()
                val isAutoAccounting = hooker.hookUtils.readData("isAutoAccounting")
                XposedBridge.log("isAutoAccounting:$isAutoAccounting")
                if(isAutoAccounting == "true"){
                    hooker.scope.launch {
                        runCatching {
                            tryStartAutoAccounting(activity)
                        }.onFailure {
                            XposedBridge.log(it)
                        }
                    }
                }

            }
        })


        XposedHelpers.findAndHookMethod("androidx.activity.ComponentActivity",classLoader, "onActivityResult",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,Intent::class.java, object : XC_MethodHook() {
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
                XposedBridge.log("requestCode:$requestCode,resultCode:$resultCode,intent:$intent")
                if(requestCode == codeAuth){
                    if(resultCode != Activity.RESULT_OK) {
                        Toast.makeText(activity, "授权失败", Toast.LENGTH_SHORT).show()
                        return
                    }
                    val resultData = intent.getStringExtra("token")
                    //调用自动记账存储
                    try {
                        AutoAccounting.setToken(activity, resultData)
                        //授权成功尝试重启
                        hooker.scope.launch {
                            runCatching {
                                tryStartAutoAccounting(activity)
                            }.onFailure {
                                XposedBridge.log(it)
                            }.onSuccess {
                                if (::autoAccounting.isInitialized){
                                   withContext(Dispatchers.Main){
                                       autoAccounting.isChecked = true
                                   }
                                }
                                hooker.hookUtils.writeData("isAutoAccounting","true")
                                syncBillsFromAutoAccounting(activity)
                            }
                        }

                    }catch (e:AutoAccountingException){
                        e.printStackTrace()
                        Toast.makeText(activity,"数据为空，可能是因为自动记账服务未启动",Toast.LENGTH_SHORT).show()
                    }

                }



            }
        })


        XposedHelpers.findAndHookMethod(clazz, "onResume",  object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val activity = param!!.thisObject as Activity
                //如果自动记账功能打开，从自动记账同步账单
                val isAutoAccounting = hooker.hookUtils.readData("isAutoAccounting")
                XposedBridge.log("isAutoAccounting:$isAutoAccounting")
                if(isAutoAccounting == "true"){
                    hooker.scope.launch {
                        syncBillsFromAutoAccounting(activity)
                    }
                }
            }
        })




    }



    private suspend fun syncBillsFromAutoAccounting(activity: Activity) = withContext(Dispatchers.IO){
       hooker.scope.launch {
           //账本等信息优先同步
           hooker.syncUtils.books()
           //同步账单【债务、报销】
           // 从自动记账同步需要记录的账单

       }

    }


    private suspend fun tryStartAutoAccounting(activity: Activity) = withContext(Dispatchers.IO) {
        AutoAccounting.init(
            activity,
            Gson().toJson(hooker.configSyncUtils.config),
        )

    }

private fun setVipName(obj: FrameLayout,classLoader: ClassLoader?){
    val vipName = hooker.hookUtils.readData("vipName")
    if(vipName!==""){
        // 调用 findViewById 并转换为 TextView
        val textView = getViewById(obj,classLoader!!,"settings_vip_badge")  as TextView
        textView.visibility = View.VISIBLE
        textView.text = vipName
    }

}

private fun hookMenu(activity: Activity,classLoader: ClassLoader?) {
        var hooked = false
    val clazz = classLoader!!.loadClass("com.mutangtech.qianji.ui.maindrawer.MainDrawerLayout")
        XposedHelpers.findAndHookMethod(
            clazz,
            "refreshAccount",
            object : XC_MethodHook(){
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    //只hook一次
                    val obj = param!!.thisObject as FrameLayout

                    setVipName(obj,classLoader)

                    if(hooked)return
                    hooked = true

                    // 调用 findViewById 并转换为 TextView
                    val linearLayout = getViewById(obj,classLoader,"main_drawer_content_layout") as LinearLayout
                    runCatching {
                        XposedHelpers.callMethod(activity.resources.assets, "addAssetPath", HookMainApp.modulePath)
                        //找到了obj里面的name字段
                        addSettingMenu(linearLayout , activity,classLoader)
                    }.onFailure {
                        XposedBridge.log(it)
                    }
                }
            })
    }


    private lateinit var autoAccounting: Switch

    private lateinit var rClass : Class<*>
    fun getViewById(obj: FrameLayout,classLoader: ClassLoader, id: String): View {
        if(!::rClass.isInitialized){
            rClass = Class.forName("com.mutangtech.qianji.R\$id", true, classLoader)
        }
        val resourceId = rClass.getField(id).getInt(null)
        // 调用 findViewById 并转换为 TextView
      return  XposedHelpers.callMethod(
            obj,
            "findViewById",
            resourceId
        ) as View
    }

    fun addSettingMenu(linearLayout: LinearLayout, context: Activity,classLoader: ClassLoader?){
        val isDarkMode: Boolean = isDarkMode(context)
        val mainColor = if (isDarkMode) -0x2c2c2d else -0xcacacb
        val subColor = if (isDarkMode) -0x9a9a9b else -0x666667
        val backgroundColor = if (isDarkMode) -0xd1d1d2 else -0x1
        val view = LayoutInflater.from(context).inflate(R.layout.item_menu, null)
        view.setBackgroundColor(backgroundColor)
        val title = view.findViewById<TextView>(R.id.title)
        title.text = context.getString(R.string.app_name)
        title.setTextColor(mainColor)
        val version = view.findViewById<TextView>(R.id.version)
        version.text = BuildConfig.VERSION_NAME
        version.setTextColor(subColor)
        view.setOnClickListener {
            //弹出自定义布局
            val menuListView = LayoutInflater.from(context).inflate(R.layout.menu_list, null)
            menuListView.setBackgroundColor(backgroundColor)
            //弹窗AlertDialog
            val isAutoAccounting = hooker.hookUtils.readData("isAutoAccounting")
            autoAccounting = menuListView.findViewById(R.id.autoAccounting)
            autoAccounting.setTextColor(mainColor)
            autoAccounting.isChecked = isAutoAccounting == "true"
            autoAccounting.setOnClickListener {
                if(autoAccounting.isChecked){
                   hooker.scope.launch {
                       runCatching {
                           tryStartAutoAccounting(context)
                           hooker.hookUtils.writeData("isAutoAccounting","true")
                       }.onFailure {
                           withContext(Dispatchers.Main){
                               autoAccounting.isChecked = false
                               val intent = Intent("net.ankio.auto.ACTION_REQUEST_AUTHORIZATION")
                               //设置包名，用于自动记账对目标app进行检查
                               intent.putExtra("packageName", hooker.packPageName)
                               try {
                                   context.startActivityForResult(intent,codeAuth)
                               }catch (e: ActivityNotFoundException){
                                   //没有自动记账，需要引导用户下载自动记账App
                                  XposedBridge.log(e)
                                   Toast.makeText(context,"未找到自动记账，请从Github下载自动记账App",Toast.LENGTH_SHORT).show()
                                  // 跳转自动记账下载页面：https://github.com/AutoAccountingOrg/AutoAccounting/
                                   val url = "https://github.com/AutoAccountingOrg/AutoAccounting/"
                                   val intentAuto = Intent(Intent.ACTION_VIEW)
                                   intentAuto.data = Uri.parse(url)
                                   try {
                                       context.startActivity(intentAuto)
                                   } catch (e: ActivityNotFoundException) {
                                       Toast.makeText(context, "没有安装任何支持的浏览器客户端", Toast.LENGTH_SHORT).show()
                                   }

                               }
                           }
                       }
                   }
                }
            }

            val title1 = menuListView.findViewById<TextView>(R.id.title1)
            title1.setTextColor(subColor)

            val title2 = menuListView.findViewById<TextView>(R.id.title2)
            title2.setTextColor(subColor)

            val title3 = menuListView.findViewById<TextView>(R.id.title3)
            title3.setTextColor(subColor)

            val title4 = menuListView.findViewById<TextView>(R.id.title4)
            title4.setTextColor(subColor)

            val editText = menuListView.findViewById<TextView>(R.id.editTextText)
            editText.setTextColor(mainColor)
            editText.text = hooker.hookUtils.readData("vipName")
            editText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hooker.hookUtils.writeData("vipName",v.text.toString())
                    setVipName(linearLayout.parent as FrameLayout,classLoader)
                }
                false
            }

            // 创建AlertDialog并设置自定义视图
            val dialog: AlertDialog = AlertDialog.Builder(context)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(context.getString(R.string.app_name))
                .setView(menuListView)
                .create()


// 显示弹窗
            dialog.show()

        }
        linearLayout.addView(view.rootView)
    }


    private fun isDarkMode(context: Context): Boolean {
        return Configuration.UI_MODE_NIGHT_YES == (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
    }

}