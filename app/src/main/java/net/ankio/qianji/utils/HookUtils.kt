package net.ankio.qianji.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import com.hjq.toast.ToastParams
import com.hjq.toast.Toaster
import com.hjq.toast.style.CustomToastStyle
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import net.ankio.qianji.HookMainApp
import net.ankio.qianji.R

class HookUtils(private val mContext: Context) {
    private val TAG = "QianjiPatch"

    fun getVersionCode(): Int {
        var appVersionCode: Long = 0
        try {
            val packageInfo: PackageInfo =
                mContext
                    .packageManager
                    .getPackageInfo(mContext.packageName, 0)
            appVersionCode =
                packageInfo.longVersionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("", e.message!!)
        }
        return appVersionCode.toInt()
    }

    fun writeData(
        key: String,
        value: String,
    ) {
        val sharedPreferences: SharedPreferences =
            mContext.getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据

        val editor = sharedPreferences.edit() // 获取编辑器

        editor.putString(key, value)

        editor.apply() // 提交修改
    }

    fun readData(key: String): String {
        val sharedPreferences: SharedPreferences =
            mContext.getSharedPreferences(TAG, Context.MODE_PRIVATE) // 私有数据
        return sharedPreferences.getString(key, "") ?: ""
    }

    fun log(
        prefix: String,
        log: String? = null,
    ) {
        if (log == null) {
            XposedBridge.log(prefix)
        } else {
            XposedBridge.log("$prefix: $log")
        }
    }

    fun toast(msg: String) {
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show()
    }

    fun addAutoContext(context: Context) {
        XposedHelpers.callMethod(
            context.resources.assets,
            "addAssetPath",
            HookMainApp.modulePath,
        )
    }

    fun toastInfo(msg: String) {
        val params = ToastParams()
        params.duration = Toast.LENGTH_LONG
        params.text = msg
        params.style = CustomToastStyle(R.layout.toast_info)
        Toaster.show(params)
    }

    fun toastError(msg: String) {
        val params = ToastParams()
        params.duration = Toast.LENGTH_LONG
        params.text = msg
        params.style = CustomToastStyle(R.layout.toast_error)
        Toaster.show(params)
    }
}
