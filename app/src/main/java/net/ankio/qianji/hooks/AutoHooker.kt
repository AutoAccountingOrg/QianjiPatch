package net.ankio.qianji.hooks

import android.content.Context
import android.content.Intent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.launch
import net.ankio.common.model.AutoBillModel
import net.ankio.qianji.api.Hooker
import net.ankio.qianji.api.PartHooker
import net.ankio.qianji.utils.QianjiBillType
import net.ankio.qianji.utils.QianjiUri
import net.ankio.qianji.utils.SyncUtils
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.Calendar

class AutoHooker(hooker: Hooker) : PartHooker(hooker) {
    override val hookName: String
        get() = "自动记账接口HooK"
    private lateinit var syncUtils: SyncUtils

    override fun onInit(
        classLoader: ClassLoader,
        context: Context,
    ) {
        syncUtils = SyncUtils.getInstance(context, classLoader, hooker)
        XposedHelpers.findAndHookMethod(
            "com.mutangtech.qianji.bill.auto.AddBillIntentAct",
            classLoader,
            "onNewIntent",
            Intent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val intent = param.args?.get(0) as Intent
                    val data = intent.data ?: return
                    val billModel = QianjiUri.parseUri(data)
                    val type = billModel.type
                    if (type == 0 || type == 5 || type == 1 || type == 2 || type == 3) {
                        return
                    }

                    // 判断是否登录，未登录用户不能使用自动记账接口

                    intent.data = null
                    param.args[0] = intent

                    // data = null 之后后续流程直接中断

                    when (type) {
                        QianjiBillType.Expend.toInt() -> {
                        }

                        QianjiBillType.Income.toInt() -> {
                        }

                        QianjiBillType.Transfer.toInt() -> {
                        }

                        QianjiBillType.ExpendReimbursement.toInt() -> {
                        }

                        QianjiBillType.ExpendLending.toInt() -> {
                            // 支出（借出）
                            // TODO
                        }

                        QianjiBillType.ExpendRepayment.toInt() -> {
                            // 支出（还款销账）
                            // TODO
                        }

                        QianjiBillType.IncomeLending.toInt() -> {
                            // 收入（借入）
                            // TODO
                        }

                        QianjiBillType.IncomeRepayment.toInt() -> {
                            // 收入（还款销账）
                            // TODO
                        }

                        QianjiBillType.IncomeReimbursement.toInt() -> {
                            // 收入（报销）
                            hooker.scope.launch {
                                incomeReimbursement(billModel, classLoader, context)
                            }
                        }
                    }
                }
            },
        )
    }

    suspend fun incomeReimbursement(
        billModel: AutoBillModel,
        classLoader: ClassLoader,
        context: Context,
    ) {
        val list = billModel.extendData.split(",")

        val billList = syncUtils.getBaoXiaoList()
        if (billList.isEmpty()) {
            // 可报销的账单为空
            return
        }

        // 根据账单id过滤出Set<Bill>，参数1
        val billIdField = syncUtils.billClazz.getDeclaredField("billId")
        billIdField.isAccessible = true
        val selectBills =
            billList.filter {
                // 反射获取it的billid属性
                val billId = (billIdField.get(it) as Long).toString()
                // 判断billId是否在list中
                list.contains(billId)
            }
        if (selectBills.isEmpty())return

        log("selectBills:$selectBills")

        // 根据收入账户获取AssetsAccount，参数2
        val assetAccounts = syncUtils.getAssetsList()
        if (assetAccounts.isEmpty())return
        val nameField = syncUtils.assetsClazz.getDeclaredField("name")
        nameField.isAccessible = true
        val assetAccount =
            assetAccounts.firstOrNull {
                val name = (nameField.get(it) as String).toString()
                name == billModel.accountNameFrom
            }

        if (assetAccount == null) {
            return
        }
        log("assetAccount:$assetAccount")

        // 报销金额，参数3
        val amount = billModel.amount.toDouble()

        // 报销时间，参数4，类型是java.util.Calendar

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = billModel.timeStamp

        // 货币类型，参数5
        val currencyExtra = classLoader.loadClass("com.mutangtech.qianji.data.model.CurrencyExtra")

        // 反射调用selectBills.first的getCurrencyExtra
        val firstBill = selectBills.first() ?: return
        val getCurrencyExtra = firstBill.javaClass.getMethod("getCurrencyExtra")
        val currencyExtraInstance = getCurrencyExtra.invoke(firstBill)

        val proxyOnGetBaoXiaoList = syncUtils.proxyOnGetBaoXiaoList
        val bxPresenterImplClazz = syncUtils.bxPresenterImplClazz
        val constructor = bxPresenterImplClazz.getDeclaredConstructor(proxyOnGetBaoXiaoList)

        val handler = InvocationHandler { _, _, _ -> }
        val proxyInstance = Proxy.newProxyInstance(classLoader, arrayOf(proxyOnGetBaoXiaoList), handler)
        val obj = constructor.newInstance(proxyInstance)

        // public void doBaoXiao(java.util.Set<? extends com.mutangtech.qianji.data.model.Bill> r39, com.mutangtech.qianji.data.model.AssetAccount r40, double r41, java.util.Calendar r43, com.mutangtech.qianji.data.model.CurrencyExtra r44) {
        //
        val asset = syncUtils.assetsClazz
        val method =
            bxPresenterImplClazz.getMethod(
                "doBaoXiao",
                Set::class.java,
                asset,
                Double::class.java,
                Calendar::class.java,
                currencyExtra,
            )

        method.invoke(obj, selectBills, assetAccount, amount, calendar, currencyExtraInstance)

        hooker.hookUtils.toast("报销成功")
    }
}
