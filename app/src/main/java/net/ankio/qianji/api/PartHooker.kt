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

import android.content.Context
import com.google.gson.Gson
import de.robv.android.xposed.XposedBridge
import net.ankio.dex.Dex
import net.ankio.dex.model.ClazzMethod
import net.ankio.qianji.utils.HookUtils

abstract class PartHooker(val hooker: Hooker) {
    abstract val hookName: String

    abstract fun onInit(
        classLoader: ClassLoader,
        context: Context,
    )

    /**
     * 正常输出日志
     */
    fun log(string: String) {
        HookUtils.log("${hooker.appName} $hookName", string)
    }

    open val methodsRule = hashMapOf<String, ClazzMethod>()
    open var method = hashMapOf<String, String>()

    fun findMethods(clazzLoader: ClassLoader): Boolean {
        val code = HookUtils.getVersionCode()
        val adaptationVersion = HookUtils.readData("methods_adaptation").toIntOrNull() ?: 0
        if (adaptationVersion == code) {
            runCatching {
                method =
                    Gson().fromJson(
                        HookUtils.readData("clazz_method"),
                        HashMap::class.java,
                    ) as HashMap<String, String>
                if (method.size != methodsRule.size) {
                    throw Exception("适配失败")
                }
            }.onFailure {
                HookUtils.writeData("methods_adaptation", "0")
                XposedBridge.log(it)
            }.onSuccess {
                return true
            }
        }
        methodsRule.forEach {
            val (clazz, methodName) = it.key.split("#")
            val loadClazz = clazzLoader.loadClass(clazz)
            val findMethod =
                Dex.findMethod(
                    loadClazz,
                    it.value,
                )
            log("findMethods findMethod:$findMethod")
            if (findMethod.isEmpty()) {
                return false
            }
            method[it.key] = findMethod
        }
        if (method.size != methodsRule.size) return false

        HookUtils.writeData("methods_adaptation", code.toString())
        HookUtils.writeData("clazz_method", Gson().toJson(method))
        return true
    }
}
