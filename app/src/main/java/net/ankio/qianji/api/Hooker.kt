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
import net.ankio.qianji.utils.HookUtils

abstract class Hooker : iHooker {
    abstract var partHookers: MutableList<PartHooker>

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
                        it.printStackTrace()
                        XposedBridge.log(it)
                        HookUtils.writeData("adaptation", "0")
                        HookUtils.writeData("methods_adaptation","0")
                    }
                }
            },
        )
    }

    fun stop() {
    }

    fun initLoadPackage(
        classLoader: ClassLoader?,
        application: Application,
    ) {
        XposedBridge.log("Welcome to 钱迹补丁")
        if (classLoader == null) {
            XposedBridge.log(this.appName + "hook失败: classloader = null")
            return
        }
        HookUtils.setApplication(application)
        if (hookLoadPackage(classLoader, application)) {
            Log.i(appName, "欢迎使用钱迹补丁，该日志表示 $appName App 已被hook。")
            for (hook in partHookers) {
                try {
                    Log.i(appName, "正在初始化Hook:${hook.hookName}...")
                    hook.onInit(classLoader, application)
                } catch (e: Exception) {
                    // 不管什么原因异常，都有可能是适配的问题，所以直接将适配版本 = 0
                    HookUtils.writeData("adaptation", "0")
                    HookUtils.writeData("methods_adaptation","0")
                    e.message?.let { Log.e("AutoAccountingError", it) }
                    Log.i(appName, "钱迹补丁Hook异常..${e.message}.")



                    Log.i(appName, "钱迹补丁重置Hook版本")
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
