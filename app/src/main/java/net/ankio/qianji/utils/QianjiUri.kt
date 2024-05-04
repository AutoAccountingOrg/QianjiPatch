package net.ankio.qianji.utils

import android.net.Uri
import net.ankio.common.model.AutoBillModel

class QianjiUri(private val billModel: AutoBillModel) {
    private val uri = StringBuilder("qianji://publicapi/addbill")

    init {
        uri.append("?type=${QianjiBillType.fromBillType(billModel.type)}")
        uri.append("&money=${billModel.amount}")
        uri.append("&time=${formatTime(billModel.timeStamp)}")
        uri.append("&remark=${urlEncode(billModel.remark)}")
        uri.append("&catename=${urlEncode(billModel.cateName)}")
        uri.append("&catechoose=0")
        if (billModel.bookName !== "日常生活") {
            uri.append("&bookname=${billModel.bookName}")
        }

        uri.append("&accountname=${billModel.accountNameFrom}")
        uri.append("&accountname2=${billModel.accountNameTo}")
        uri.append("&fee=${billModel.fee}")
        // 自动记账添加的拓展字段
        uri.append("&extendData=${billModel.extendData}")

        uri.append("&showresult=0")
    }

    fun getUri(): Uri {
        return Uri.parse(uri.toString())
    }

    private fun urlEncode(str: String): String {
        return java.net.URLEncoder.encode(str, "UTF-8")
    }

    private fun formatTime(time: Long): String {
        // 时间格式为yyyy-MM-dd HH:mm:ss
        val date = java.util.Date(time)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return sdf.format(date)
    }
}
