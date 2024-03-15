package net.ankio.qianji.hooks

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.sdk.AutoAccounting
import net.ankio.auto.sdk.exception.AutoAccountingException
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
                                    autoAccounting.isChecked = true
                                }
                                hooker.hookUtils.writeData("isAutoAccounting","true")
                                //授权成功后可以一次性将所有数据先同步给自动记账
                                //TODO 钱迹同步数据给自动记账
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
                    syncBillsFromAutoAccounting(activity)
                }
            }
        })




    }



    private fun syncBillsFromAutoAccounting(activity: Activity){

    }


    private suspend fun tryStartAutoAccounting(activity: Activity) {
        AutoAccounting.init(
            activity,
            Gson().toJson(AutoAccounting()),
        )
    }



private fun hookMenu(activity: Activity,classLoader: ClassLoader?) {
        var hooked = false
        XposedHelpers.findAndHookMethod(
            "com.mutangtech.qianji.ui.maindrawer.MainDrawerLayout",
            classLoader ,
            "refreshAccount",
            object : XC_MethodHook(){
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    if(hooked)return
                    hooked = true
                    //只hook一次
                    val obj = param!!.thisObject as FrameLayout

                    hooker.hookUtils.findField(obj){ name, value ->
                        if(value is LinearLayout){
                            kotlin.runCatching {
                                //获取Activity
                              //  val activity = XposedHelpers.callMethod(obj, "getContext") as Context
                                XposedHelpers.callMethod(activity.resources.assets, "addAssetPath", HookMainApp.modulePath)
                                addSettingMenu(value , activity,classLoader)
                            }.onFailure {
                                XposedBridge.log(it)
                            }
                            return@findField true
                        }
                        false
                    }
                }
            })
    }


    private lateinit var autoAccounting: Switch


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
            //弹窗AlertDialog
            val isAutoAccounting = hooker.hookUtils.readData("isAutoAccounting")
            autoAccounting = menuListView.findViewById<Switch>(R.id.autoAccounting)
            autoAccounting.isChecked = isAutoAccounting == "true"
            autoAccounting.setOnClickListener {
                if(autoAccounting.isChecked){
                   hooker.scope.launch {
                       runCatching {
                           tryStartAutoAccounting(context)
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
                                   e.printStackTrace()
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


    fun isDarkMode(context: Context): Boolean {
        return Configuration.UI_MODE_NIGHT_YES == (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
    }

}