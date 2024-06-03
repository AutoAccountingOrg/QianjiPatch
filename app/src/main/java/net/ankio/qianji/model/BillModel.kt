package net.ankio.qianji.model

class BillModel(
    var amount: Double = 0.0, // 金额
    var time: Long = 0, // 时间
    var remark: String = "", // 备注
    var id: String = "", // 账单id，自动记账进行报销、销账的时候需要用到
    var type: Int = 0, // 账单类型，只有 0 支出 1 收入，（包括报销、债务
    var book: String = "", // 账本名称
    var category: String = "", // 分类名称
    var accountFrom: String = "", // 转出账户名称
    var accountTo: String = "", // 转入账户名称
)
