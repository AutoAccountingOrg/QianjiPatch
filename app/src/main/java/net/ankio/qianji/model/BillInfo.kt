package net.ankio.qianji.model

class BillInfo {
    var id = 0

    /**
     * 账单类型 只有三种
     */
    var type: Int = 0

    /**
     * 币种类型
     */
    var currency: String = ""

    /**
     * 金额 大于0
     */
    var money: Int = 0

    /**
     * 手续费
     */
    var fee: Int = 0

    /**
     * 记账时间
     * yyyy-MM-dd HH:mm:ss
     */
    var timeStamp: Long = 0

    /**
     * 商户名称
     */
    var shopName: String = ""

    /**
     * 商品名称
     */
    var shopItem: String = ""

    /**
     * 分类名称
     */
    var cateName: String = "其他"

    /**
     * 拓展数据域，如果是报销或者销账，会对应账单ID
     */
    var extendData: String = ""

    /**
     * 账本名称
     */
    var bookName: String = "默认账本"

    /**
     * 账单所属资产名称（或者转出账户）
     */
    var accountNameFrom: String = ""

    /**
     * 转入账户
     */
    var accountNameTo = ""

    /**
     * 这笔账单的来源,例如是微信还是支付宝
     */
    var from = ""

    /**
     * 来源类型，app、无障碍、通知、短信
     */
    var fromType: Int = 0

    /**
     * 分组id，这个id是指将短时间内捕获到的同等金额进行合并的分组id
     */
    var groupId: Int = 0

    /**
     * 数据渠道，这里指的是更具体的渠道，例如【建设银行】微信公众号，用户【xxxx】这种
     */
    var channel: String = ""

    /**
     * 是否已从App同步
     */
    var syncFromApp: Boolean = false

    /**
     * 备注信息
     */
    var remark: String = ""
}
