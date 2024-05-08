package net.ankio.qianji.utils

import de.robv.android.xposed.XposedHelpers
import net.ankio.qianji.api.Hooker

object UserUtils {
    fun isLogin(hooker: Hooker): Boolean {
        val clazz = hooker.loadClazz["UserManager"]
        val obj = XposedHelpers.callStaticMethod(clazz, "getInstance")
        return XposedHelpers.callMethod(obj, "isLogin") as Boolean
    }
}
