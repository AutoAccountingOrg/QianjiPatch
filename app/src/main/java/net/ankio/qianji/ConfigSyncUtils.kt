package net.ankio.qianji

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.launch
import net.ankio.auto.sdk.AutoAccounting
import net.ankio.common.config.AccountingConfig
import net.ankio.qianji.api.Hooker

class ConfigSyncUtils(val context: Context,val hooker: Hooker) {
    var config = runCatching {
       Gson().fromJson(hooker.hookUtils.readData("config"),AccountingConfig::class.java)
   }.getOrElse {
       AccountingConfig()
   }

    private suspend fun saveAndSync(){
        val  configText = Gson().toJson(config)
        hooker.hookUtils.writeData("config",configText)
        AutoAccounting.setConfig(context,configText)
    }

    fun setAssetManagement(boolean: Boolean){
        config.assetManagement = boolean
        hooker.scope.launch {
            saveAndSync()
        }
    }

    fun setMultiCurrency(boolean: Boolean){
        config.multiCurrency = boolean
        hooker.scope.launch {
            saveAndSync()
        }
    }

    fun setReimbursement(boolean: Boolean){
        config.reimbursement = boolean
        hooker.scope.launch {
            saveAndSync()
        }
    }

    fun setLending(boolean: Boolean){
        config.lending = boolean
        hooker.scope.launch {
            saveAndSync()
        }
    }

    fun setMultiBooks(boolean: Boolean){
        config.multiBooks = boolean
        hooker.scope.launch {
            saveAndSync()
        }
    }

    fun setFee(boolean: Boolean){
        config.fee = boolean
        hooker.scope.launch {
            saveAndSync()
        }
    }


}