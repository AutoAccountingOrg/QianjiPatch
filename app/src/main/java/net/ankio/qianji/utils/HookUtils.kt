package net.ankio.qianji.utils

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Field
import java.lang.reflect.Modifier


class HookUtils(private val mContext: Context) {
    private val TAG = "QianjiPatch"


    fun getVersionCode(): Int {
        return kotlin.runCatching {
            mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionCode
        }.getOrElse {
            log("获取版本号失败",it.message)
            0
        }
    }

    fun writeData(key: String, value: String) {
        val sharedPreferences: SharedPreferences =
            mContext.getSharedPreferences(TAG, Context.MODE_PRIVATE) //私有数据

        val editor = sharedPreferences.edit() //获取编辑器

        editor.putString(key, value)

        editor.apply() //提交修改
    }


    fun readData(key: String): String {
        val sharedPreferences: SharedPreferences =
            mContext.getSharedPreferences(TAG, Context.MODE_PRIVATE) //私有数据
        return sharedPreferences.getString(key, "")?:""
    }

    fun log(prefix:String,log:String?=null){
        if(log == null)
            XposedBridge.log(prefix)
        else
            XposedBridge.log("$prefix: $log")
    }


    fun toast(msg:String){
        Toast.makeText(mContext,msg,Toast.LENGTH_LONG).show()
    }


    private fun getKey(key:String = ""): String {
        return "${ getVersionCode()}_${key}"
    }


    fun getValue(key:String = ""):String{
        return readData(getKey(key))
    }

    fun setValue(key:String = "",value:String){
        writeData(getKey(key),value)
    }
}