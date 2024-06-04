package net.ankio.qianji.utils

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.hjq.toast.ToastParams
import com.hjq.toast.Toaster
import com.hjq.toast.style.CustomToastStyle
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import net.ankio.qianji.HookMainApp
import net.ankio.qianji.R
import net.ankio.qianji.server.AutoServer
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HookUtils {
    private lateinit var application: Application
    private lateinit var server: AutoServer
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun getJob(): Job {
        return job
    }

    fun getScope(): CoroutineScope {
        return scope
    }

    fun getApplication(): Application {
        return application
    }

    fun setApplication(application: Application) {
        this.application = application
        addAutoContext(application)
        server = AutoServer()
    }

    fun getVersionCode(): Int {
        return runCatching {
            application.packageManager.getPackageInfo(application.packageName, 0).versionCode
        }.getOrElse {
            0
        }
    }

    private val tag = "autoAccountingConfig"

    fun writeData(
        key: String,
        value: String,
    ) {
        val sharedPreferences: SharedPreferences =
            application.getSharedPreferences(tag, Context.MODE_PRIVATE) // 私有数据

        val editor = sharedPreferences.edit() // 获取编辑器

        editor.putString(key, value)

        editor.apply() // 提交修改
    }

    fun readData(key: String): String {
        val sharedPreferences: SharedPreferences =
            application.getSharedPreferences(tag, Context.MODE_PRIVATE) // 私有数据
        return sharedPreferences.getString(key, "") ?: ""
    }

    fun log(
        prefix: String,
        log: String? = null,
    ) {
        if (log == null) {
            XposedBridge.log(prefix)
            Logger.i(prefix)
        } else {
            XposedBridge.log("$prefix: $log")
            Logger.i("$prefix: $log")
        }
    }

    fun toast(msg: String) {
        Toast.makeText(application, msg, Toast.LENGTH_LONG).show()
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

    fun md5(data: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(data.toByteArray())).toString(16).padStart(32, '0')
    }

    fun getService(): AutoServer {
        return server
    }

    fun currentTimeToDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(System.currentTimeMillis()))
    }
}
