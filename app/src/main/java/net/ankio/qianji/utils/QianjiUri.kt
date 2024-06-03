package net.ankio.qianji.utils

import android.net.Uri
import net.ankio.qianji.model.AccountingConfig
import net.ankio.qianji.model.BillInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QianjiUri(private val billModel: BillInfo, config: AccountingConfig) {
    private val uri = StringBuilder("qianji://publicapi/addbill")

    init {
        uri.append("?type=${QianjiBillType.fromBillType(billModel.type)}")
        uri.append("&money=${billModel.money}")
        uri.append("&time=${formatTime(billModel.timeStamp)}")
        uri.append("&remark=${urlEncode(billModel.remark)}")
        var category = billModel.cateName
        if (billModel.cateName.contains("-")) {
            val categoryNames = billModel.cateName.split("-")
            category = "${categoryNames[0]}/::/${categoryNames[1]}"
        }
        uri.append("&catename=${urlEncode(category)}")
        uri.append("&catechoose=0")

        if (config.multiBooks) {
            if (billModel.bookName != "默认账本" && billModel.bookName != "日常账本") {
                uri.append("&bookname=${urlEncode(billModel.bookName)}")
            }
        }

        if (config.assetManagement || config.lending) {
            uri.append("&accountname=${urlEncode(billModel.accountNameFrom)}")
            uri.append("&accountname2=${urlEncode(billModel.accountNameTo)}")
        }

        if (config.fee) {
            uri.append("&fee=${billModel.fee}")
        }

        if (config.multiCurrency) {
            uri.append("&currency=${billModel.currency}")
        }
        // 自动记账添加的拓展字段
        uri.append("&extendData=${billModel.extendData}")

        uri.append("&showresult=0")
    }

    fun getUri(): Uri {
        return Uri.parse(uri.toString())
    }

    companion object {
        fun dateToStamp(
            time: String,
            format: String,
        ): Long {
            val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
            try {
                val date: Date = checkNotNull(simpleDateFormat.parse(time))
                return date.time
            } catch (e: Throwable) {
                return 0
            }
        }

        fun parseUri(uri: Uri): BillInfo {
            val type = uri.getQueryParameter("type")?.toInt() ?: 0
            val amount = uri.getQueryParameter("money")?.toFloat() ?: 0.0F
            val time = dateToStamp(uri.getQueryParameter("time")!!, "yyyy-MM-dd HH:mm:ss")
            val remark = uri.getQueryParameter("remark") ?: ""
            val cateName = uri.getQueryParameter("catename") ?: ""
            val bookName = uri.getQueryParameter("bookname") ?: "日常生活"
            val accountNameFrom = uri.getQueryParameter("accountname") ?: ""
            val accountNameTo = uri.getQueryParameter("accountname2") ?: ""
            val fee = uri.getQueryParameter("fee")?.toFloat() ?: 0.0F
            val extendData = uri.getQueryParameter("extendData") ?: ""
            val billInfo = BillInfo()
            billInfo.type = type
            billInfo.money = amount.toInt()
            billInfo.timeStamp = time
            billInfo.remark = remark
            billInfo.cateName = cateName
            billInfo.bookName = bookName
            billInfo.accountNameFrom = accountNameFrom
            billInfo.accountNameTo = accountNameTo
            billInfo.fee = fee.toInt()
            billInfo.extendData = extendData
            return billInfo
        }
    }

    private fun urlEncode(str: String): String {
        return java.net.URLEncoder.encode(str, "UTF-8")
    }

    private fun formatTime(time: Long): String {
        // 时间格式为yyyy-MM-dd HH:mm:ss
        val date = Date(time)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return sdf.format(date)
    }
}
