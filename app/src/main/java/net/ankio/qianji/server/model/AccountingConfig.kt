package net.ankio.qianji.server.model

import com.google.gson.Gson
import de.robv.android.xposed.XposedBridge
import net.ankio.qianji.utils.HookUtils

data class AccountingConfig(
    var assetManagement: Boolean = false, // 是否开启资产管理
    var multiCurrency: Boolean = false, // 是否开启多币种
    var reimbursement: Boolean = false, // 是否开启报销
    var lending: Boolean = false, // 是否开启债务功能
    var multiBooks: Boolean = false, // 是否开启多账本
    var fee: Boolean = false, // 是否开启手续费
){
    companion object{
        fun getConfig(): AccountingConfig =  runCatching {
            Gson().fromJson(HookUtils.readData("config"), AccountingConfig::class.java)
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

        suspend fun saveAndSync(config: AccountingConfig) {
            runCatching {
                val configText = Gson().toJson(config)
                HookUtils.writeData("config", configText)
                SettingModel.set(SettingModel().apply {
                    key = "config"
                    app= "server"
                    value = configText
                })
                HookUtils.log("Config", configText)
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    }
}
