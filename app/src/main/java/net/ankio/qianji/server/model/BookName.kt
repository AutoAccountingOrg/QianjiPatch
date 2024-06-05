/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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
package net.ankio.qianji.server.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.qianji.utils.HookUtils

class BookName {
    // 账本列表
    var category: ArrayList<Category> = arrayListOf()

    /**
     * 账户名
     */
    var name: String = ""

    /**
     * 图标是url或base64编码字符串
     */
    var icon: String = "" // 图标

    companion object {
        suspend fun sync2Server(
            data: List<BookName>,
            md5: String,
        ) = withContext(Dispatchers.IO) {
            HookUtils.getService().sendMsg(
                "book/sync",
                hashMapOf(
                    "data" to data,
                    "md5" to md5,
                ),
            )
        }
    }
}
