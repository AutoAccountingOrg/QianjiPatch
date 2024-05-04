package net.ankio.qianji.hooks

import android.content.Context
import net.ankio.qianji.api.Hooker
import net.ankio.qianji.api.PartHooker

class AutoHooker(hooker: Hooker) : PartHooker(hooker) {
    override val hookName: String
        get() = "自动记账接口HooK"

    override fun onInit(
        classLoader: ClassLoader,
        context: Context,
    ) {
        // 去除时间校验
    }
}
