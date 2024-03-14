package net.ankio.qianji.hooks

import android.content.Context
import net.ankio.qianji.api.Hooker
import net.ankio.qianji.api.PartHooker

class QianjiHooker: Hooker(){
    override val packPageName: String = "com.mutangtech.qianji"
    override val appName: String = "钱迹"
    override var partHookers: MutableList<PartHooker> = arrayListOf(

    )

    override fun hookLoadPackage(classLoader: ClassLoader?, context: Context?) {

    }

}