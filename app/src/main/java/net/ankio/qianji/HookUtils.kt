package net.ankio.qianji

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
     fun printMethodsRecursively(clazz: Class<*>?) {
        if (clazz == null || clazz == Any::class.java) {
            return
        }

        XposedBridge.log("Methods of class: " + clazz.name)

        val methods = clazz.declaredMethods
        for (method in methods) {
            val sb = StringBuilder()
            sb.append(Modifier.toString(method.modifiers)).append(" ")
            sb.append(method.returnType.name).append(" ")
            sb.append(method.name).append("(")
            val parameterTypes = method.parameterTypes
            for (i in parameterTypes.indices) {
                sb.append(parameterTypes[i].name)
                if (i < parameterTypes.size - 1) {
                    sb.append(", ")
                }
            }
            sb.append(")")
            XposedBridge.log(sb.toString())
        }

        // 递归打印父类的方法
        printMethodsRecursively(clazz.superclass)
    }
    fun findField(classObj: Any,callback:(String,Any?)->Boolean) {
        val fields: Array<Field> = classObj.javaClass.declaredFields
        for (field in fields) {
            field.isAccessible = true
            if(callback(field.name,field.get(classObj))){
                break
            }

        }
    }

    fun dumpFields(classObj: Any?, dumpClass: Class<*>) {
        log("开始获取对象中的属性值：" + dumpClass.simpleName)
        try {
            val fields: Array<Field> = dumpClass.declaredFields
            for (field in fields) {
                field.isAccessible = true
                log(field.getName(), java.lang.String.valueOf(field.get(classObj)))
            }
            log("dump完成：")
        } catch (e: Exception) {
            log("dump失败：", e.toString())
        }
    }

    fun dumpFields(classObj: Any) {
        dumpFields(classObj, classObj.javaClass)
    }
    // 获取指定名称的类声明的类成员变量、类方法、内部类的信息
    fun dumpClass(actions: Class<*>) {
        XposedBridge.log("[Ankio]" + "Dump class " + actions.name)
        XposedBridge.log("[Ankio]" + "Methods")
        // 获取到指定名称类声明的所有方法的信息
        val m = actions.declaredMethods
        // 打印获取到的所有的类方法的信息
        for (method in m) {
            XposedBridge.log(method.toString())
        }
        XposedBridge.log("[Ankio]Fields")
        // 获取到指定名称类声明的所有变量的信息
        val f: Array<Field> = actions.declaredFields
        // 打印获取到的所有变量的信息
        for (field in f) {
            XposedBridge.log("[Ankio]$field")
        }
        XposedBridge.log("[Ankio]Classes")
        // 获取到指定名称类中声明的所有内部类的信息
        val c = actions.declaredClasses
        // 打印获取到的所有内部类的信息
        for (aClass in c) {
            XposedBridge.log("[Ankio]$aClass")
        }
    }


    fun toast(msg: String?) {
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show()
    }
}