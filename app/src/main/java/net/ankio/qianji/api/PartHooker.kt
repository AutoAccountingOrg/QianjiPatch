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
import android.util.Log
import net.ankio.qianji.HookMainApp

abstract class PartHooker(val hooker: Hooker) {
    abstract  val hookName: String

    var code = "0"

    abstract fun onInit(classLoader: ClassLoader?,context: Context?)

    /**
     * 正常输出日志
     */
    fun log(string: String){
        hooker.hookUtils.log(HookMainApp.getTag(hooker.appName, getSimpleName()), string)
    }

    private fun getSimpleName(): String {
        val stackTrace = Thread.currentThread().stackTrace
        val callerClassName = if (stackTrace.size >= 5) {
            stackTrace[4].className
        } else {
            "Unknown"
        }
        return callerClassName.substringAfterLast('.') // 获取简单类名
    }

    private fun getKey(key:String = ""): String {
        return "${hookName}_${code}_${key}"
    }


    fun getValue(key:String = ""):String{
        return hooker.hookUtils.readData(getKey(key))
    }

    fun setValue(key:String = "",value:String){
        hooker.hookUtils.writeData(getKey(key),value)
    }


}