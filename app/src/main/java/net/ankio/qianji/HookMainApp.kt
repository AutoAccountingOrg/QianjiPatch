package net.ankio.qianji

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import net.ankio.qianji.api.Hooker
import net.ankio.qianji.hooks.QianjiHooker

class HookMainApp : IXposedHookLoadPackage, IXposedHookZygoteInit {
    companion object {
        var modulePath = ""
    }

    private var mHookList: MutableList<Hooker> =
        arrayListOf(
            QianjiHooker(),
        )

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        for (hook in mHookList) {
            try {
                hook.onLoadPackage(lpparam)
            } catch (e: Exception) {
                e.message?.let { Log.e("钱迹", it) }
                println(e)
                XposedBridge.log(e.message)
                hook.stop()
            }
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        modulePath = startupParam?.modulePath ?: ""
    }
}
