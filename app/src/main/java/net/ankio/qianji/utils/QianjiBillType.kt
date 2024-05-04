package net.ankio.qianji.utils

enum class QianjiBillType(private val value: Int) {
    Expend(0), // 支出
    Income(1), // 收入
    Transfer(2), // 转账
    ExpendReimbursement(5), // 支出（记作报销）

    // 以下是钱迹未实现的功能

    ExpendLending(15), // 支出（借出）
    ExpendRepayment(16), // 支出（还款销账）

    IncomeLending(17), // 收入（借入）
    IncomeRepayment(18), // 收入（还款销账）
    IncomeReimbursement(19), // 收入（报销）
    ;

    fun toInt(): Int = value

    companion object {
        // 将整数转换为枚举值
        fun fromBillType(value: Int): Int {
            return when (value) {
                0 -> Expend.toInt()
                1 -> Income.toInt()
                2 -> Transfer.toInt()
                4 -> ExpendReimbursement.toInt()
                5 -> ExpendLending.toInt()
                6 -> ExpendRepayment.toInt()
                7 -> IncomeLending.toInt()
                8 -> IncomeRepayment.toInt()
                9 -> IncomeReimbursement.toInt()
                else -> Expend.toInt()
            }
        }
    }
}
