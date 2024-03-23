package net.ankio.qianji.hooks

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import net.ankio.qianji.api.Hooker
import net.ankio.qianji.api.PartHooker

class BookHooker(hooker: Hooker):PartHooker(hooker) {
    override val hookName: String = "多账本管理Hook"

    override fun onInit(classLoader: ClassLoader?, context: Context?) {
        val clazzName = hooker.clazz["BookManager"]
        val clazz = classLoader?.loadClass(clazzName)
        val bookClazz = classLoader?.loadClass("com.mutangtech.qianji.data.model.Book")
        //增加账本的时候同步账本到自动记账
        XposedHelpers.findAndHookMethod(clazz,"add",bookClazz,object :XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                //TODO 增加账本的时候同步账本到自动记账
              //  hooker.syncUtils.books()
            }
        })
        //删除账本的时候同步账本到自动记账
        XposedHelpers.findAndHookMethod(clazz,"delete",bookClazz,object :XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                //TODO 删除账本的时候同步账本到自动记账
                //hooker.syncUtils.books()
            }
        })

        //更新账本的时候同步账本到自动记账
        XposedHelpers.findAndHookMethod(clazz,"update",bookClazz,object :XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                //TODO 更新账本的时候同步账本到自动记账
               // hooker.syncUtils.books()
            }
        })

    }

}