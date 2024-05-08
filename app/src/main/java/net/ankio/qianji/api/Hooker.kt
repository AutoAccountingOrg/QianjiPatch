/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.qianji.api

/*
 * Copyright (C) 2021 dreamn(dream@dreamn.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import android.app.Application
import android.content.Context
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import net.ankio.qianji.HookMainApp
import net.ankio.qianji.utils.ConfigSyncUtils
import net.ankio.qianji.utils.HookUtils

abstract class Hooker : iHooker {
    abstract var partHookers: MutableList<PartHooker>
    lateinit var hookUtils: HookUtils
    lateinit var configSyncUtils: ConfigSyncUtils
    private var TAG = "QianjiPatch"
    private lateinit var job: Job

    lateinit var scope: CoroutineScope

    private fun hookMainInOtherAppContext() {
        var hookStatus = false
        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "attach",
            Context::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    if (hookStatus) {
                        return
                    }
                    hookStatus = true
                    val context = param.args[0] as Context
                    val application = param.thisObject as Application
                    runCatching {
                        initLoadPackage(context.classLoader, application)
                    }.onFailure {
                        XposedBridge.log("钱迹补丁Hook异常..${it.message}.")
                        Log.e("钱迹补丁Hook异常", it.message.toString())
                        it.printStackTrace()
                        XposedBridge.log(it)
                    }
                }
            },
        )
    }

    fun stop() {
        if (::job.isInitialized) {
            job.cancel()
        }
    }

    fun initLoadPackage(
        classLoader: ClassLoader?,
        application: Application,
    ) {
        XposedBridge.log("[$TAG] Welcome to 钱迹补丁")
        Log.i(TAG, "[$TAG] Welcome to 钱迹补丁")
        if (classLoader == null) {
            XposedBridge.log("[AutoAccounting]" + this.appName + "hook失败: classloader 或 context = null")
            return
        }
        hookUtils = HookUtils(application)

        job = Job()
        scope = CoroutineScope(Dispatchers.IO + job)
        configSyncUtils = ConfigSyncUtils(application, this)

        if (hookLoadPackage(classLoader, application)) {
            Log.i(HookMainApp.getTag(appName, packPageName), "欢迎使用钱迹补丁，该日志表示 $appName App 已被hook。")
            for (hook in partHookers) {
                try {
                    Log.i(HookMainApp.getTag(appName, packPageName), "正在初始化Hook:${hook.hookName}...")
                    hook.onInit(classLoader, application)
                } catch (e: Exception) {
                    e.message?.let { Log.e("AutoAccountingError", it) }
                    Log.i(HookMainApp.getTag(), "钱迹补丁Hook异常..${e.message}.")
                    XposedBridge.log(e)
                    // 不管什么原因异常，都有可能是适配的问题，所以直接将适配版本 = 0
                    hookUtils.writeData("adaptation", "0")
                }
            }
        }
    }

    @Throws(ClassNotFoundException::class)
    abstract fun hookLoadPackage(
        classLoader: ClassLoader,
        context: Context,
    ): Boolean

    override fun onLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        val pkg = lpparam?.packageName
        val processName = lpparam?.processName
        if (lpparam != null) {
            if (!lpparam.isFirstApplication) return
        }
        if (pkg != packPageName || processName != packPageName) return
        hookMainInOtherAppContext()
    }
}
