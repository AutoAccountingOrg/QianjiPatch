package net.ankio.qianji.utils

import net.ankio.common.constant.BillType

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
                BillType.Expend.ordinal -> Expend.toInt()
                BillType.Income.ordinal -> Income.toInt()
                BillType.Transfer.ordinal -> Transfer.toInt()
                BillType.ExpendReimbursement.ordinal -> ExpendReimbursement.toInt()
                BillType.ExpendLending.ordinal -> ExpendLending.toInt()
                BillType.ExpendRepayment.ordinal -> ExpendRepayment.toInt()
                BillType.IncomeLending.ordinal -> IncomeLending.toInt()
                BillType.IncomeRepayment.ordinal -> IncomeRepayment.toInt()
                BillType.IncomeReimbursement.ordinal -> IncomeReimbursement.toInt()
                else -> Expend.toInt()
            }
        }
    }
}
