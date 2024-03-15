package net.ankio.qianji.hooks

import android.content.Context
import android.util.Log
import android.view.WindowInsets.Side
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import net.ankio.qianji.HookMainApp
import net.ankio.qianji.api.Hooker
import net.ankio.qianji.api.PartHooker

class QianjiHooker: Hooker(){
    override val packPageName: String = "com.mutangtech.qianji"
    override val appName: String = "钱迹"
    override var partHookers: MutableList<PartHooker> = arrayListOf(
        SidePartHooker(this)
    )


    override fun hookLoadPackage(classLoader: ClassLoader?, context: Context?) {

        val code = hookUtils.getVersionCode().toString()
        //刚进入App
        partHookers.forEach {
            it.code = code
        }
    }



}