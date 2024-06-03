package net.ankio.qianji.utils

import android.content.Context
import com.google.gson.Gson
import de.robv.android.xposed.XposedBridge
import net.ankio.qianji.api.Hooker
import net.ankio.qianji.model.AccountingConfig
import java.lang.RuntimeException

class ConfigSyncUtils(val context: Context, val hooker: Hooker) {
    var api = hooker.hookUtils.getAutoAccounting()
    var config: AccountingConfig =
        runCatching {
            Gson().fromJson(hooker.hookUtils.readData("config"), AccountingConfig::class.java)
                ?: throw RuntimeException("config is null")
        }.getOrElse {
            AccountingConfig(
                assetManagement = true,
                multiCurrency = true,
                reimbursement = true,
                lending = true,
                multiBooks = true,
                fee = true,
            )
        }

    suspend fun saveAndSync() {
        runCatching {
            val configText = Gson().toJson(config)
            hooker.hookUtils.writeData("config", configText)
            api.setConfig(configText)
            hooker.hookUtils.log("Config", configText)
        }.onFailure {
            XposedBridge.log(it)
        }
    }
}
