package net.ankio.qianji.utils

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.ankio.auto.sdk.AutoAccounting
import net.ankio.auto.sdk.exception.AutoAccountingException
import net.ankio.common.constant.AssetType
import net.ankio.common.constant.BillType
import net.ankio.common.constant.Currency
import net.ankio.common.model.AssetsModel
import net.ankio.common.model.AutoBillModel
import net.ankio.common.model.BillModel
import net.ankio.common.model.BookModel
import net.ankio.common.model.CategoryModel
import net.ankio.qianji.api.Hooker
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 用于将钱迹的数据同步给自动记账
 */
class SyncUtils(val context: Context, private val classLoader: ClassLoader, private val hooker: Hooker) {
    /**
     * 账本管理
     */
    private var bookManager: Any =
        XposedHelpers.callStaticMethod(
            hooker.loadClazz["BookManager"],
            "getInstance",
        )

    /**
     * 账本模型
     */
    private var bookClazz: Class<*> = classLoader.loadClass("com.mutangtech.qianji.data.model.Book")

    /**
     * 获取到分类之后的处理类
     */
    private var proxyOnGetCategoryListClazz: Class<*> = hooker.loadClazz["onGetCategoryList"]!!

    /**
     * 分类初始化
     */
    private var cateInitPresenterImplClazz: Class<*> =
        classLoader.loadClass(
            "com.mutangtech.qianji.bill.add.category.CateInitPresenterImpl",
        )

    /**
     * 分类模型
     */
    private var category: Class<*> = classLoader.loadClass("com.mutangtech.qianji.data.model.Category")

    /**
     * 资产模型
     */
    var assetsClazz: Class<*> = classLoader.loadClass("com.mutangtech.qianji.data.model.AssetAccount")

    /**
     * 获取到资产之后的处理类
     */
    private var proxyOnGetAssetsClazz: Class<*> = hooker.loadClazz["onGetAssetsFromApi"]!!

    /**
     * 资产管理类
     */
    private var assetPreviewPresenterImplClazz: Class<*> =
        classLoader.loadClass(
            "com.mutangtech.qianji.asset.account.mvp.AssetPreviewPresenterImpl",
        )

    /**
     * 账单模型
     */
    var billClazz: Class<*> = classLoader.loadClass("com.mutangtech.qianji.data.model.Bill")

    /**
     * 获取到报销列表后的处理类
     */
    var proxyOnGetBaoXiaoList: Class<*> = hooker.loadClazz["onGetBaoXiaoList"]!!

    /**
     * 报销管理
     */
    var bxPresenterImplClazz: Class<*> = classLoader.loadClass("com.mutangtech.qianji.bill.baoxiao.BxPresenterImpl")

    /**
     * 报销过滤的枚举类型
     */
    private var enumFilter: Class<*> = hooker.loadClazz["filterEnum"]!!

    /**
     * 账本枚举类型
     */
    private var bookFilter: Class<*> = classLoader.loadClass("com.mutangtech.qianji.filter.filters.BookFilter")

    /**
     * 关键词枚举
     */
    private var keywordFilter: Class<*> = classLoader.loadClass("com.mutangtech.qianji.filter.filters.KeywordFilter")

    companion object {
        @Volatile
        private var instance: SyncUtils? = null

        fun getInstance(
            context: Context,
            classLoader: ClassLoader,
            hooker: Hooker,
        ): SyncUtils =
            instance ?: synchronized(this) {
                instance ?: SyncUtils(context, classLoader, hooker).also { instance = it }
            }
    }

    /**
     * 获取分类列表
     */
    suspend fun getCategoryList(bookId: Long): HashMap<String, Any> =
        suspendCoroutine { continuation ->
            val handler =
                InvocationHandler { _, method, args ->
                    if (method.name == "onGetCategoryList") {
                        val list1 = args[0]
                        val list2 = args[1]
                        runCatching {
                            continuation.resume(hashMapOf("list1" to list1, "list2" to list2))
                        }
                    }
                    null
                }
            val proxyInstance =
                Proxy.newProxyInstance(classLoader, arrayOf(proxyOnGetCategoryListClazz), handler)
            val constructor =
                cateInitPresenterImplClazz.getDeclaredConstructor(proxyOnGetCategoryListClazz)
            val obj = constructor.newInstance(proxyInstance)
            val loadCategoryListMethod =
                cateInitPresenterImplClazz.getMethod(
                    "loadCategoryList",
                    Long::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                )
            loadCategoryListMethod.invoke(obj, bookId, false)
        }

    /**
     * 获取资产列表
     */
    suspend fun getAssetsList(): List<*> =
        suspendCoroutine { continuation ->
            var resumed = false
            val handler =
                InvocationHandler { _, method, args ->
                    if (method.name == "onGetAssetsFromApi") {
                        val z10 = args[0]
                        val accounts = args[1]
                        val hashMap = args[2]
                        XposedBridge.log("账户信息:${Gson().toJson(accounts)},z10:${Gson().toJson(z10)},hashMap:${Gson().toJson(hashMap)}")

                        if (accounts !== null && !resumed) {
                            resumed = true
                            continuation.resume(accounts as List<*>)
                        }
                    } else if (method.name == "onGetAssetsFromDB") {
                        val z10 = args[1]
                        val accounts = args[0]
                        val hashMap = args[2]
                        XposedBridge.log("账户信息2:${Gson().toJson(accounts)},z10:${Gson().toJson(z10)},hashMap:${Gson().toJson(hashMap)}")
                        if (accounts !== null && !resumed) {
                            resumed = true
                            continuation.resume(accounts as List<*>)
                        }
                    }
                    null
                }
            val proxyInstance =
                Proxy.newProxyInstance(classLoader, arrayOf(proxyOnGetAssetsClazz), handler)
            val constructor =
                assetPreviewPresenterImplClazz.getDeclaredConstructor(proxyOnGetAssetsClazz)
            val obj = constructor.newInstance(proxyInstance)
            val loadCategoryListMethod =
                assetPreviewPresenterImplClazz.getMethod(
                    "loadAssets",
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                )
            loadCategoryListMethod.invoke(obj, true, false)
        }

    /**
     * 获取报销账单列表
     */
    suspend fun getBaoXiaoList(all: Boolean = false): List<*> =
        suspendCoroutine { continuation ->

            val handler =
                InvocationHandler { _, method, args ->
                    if (method.name == "onGetList") {
                        val billList = args[0]
                        continuation.resume(billList as List<*>)
                    }
                    null
                }
            val proxyInstance =
                Proxy.newProxyInstance(classLoader, arrayOf(proxyOnGetBaoXiaoList), handler)
            val constructor = bxPresenterImplClazz.getDeclaredConstructor(proxyOnGetBaoXiaoList)
            val obj = constructor.newInstance(proxyInstance)
            val refreshMethod =
                bxPresenterImplClazz.getMethod("refresh", enumFilter, bookFilter, keywordFilter)

            val enumConstants = enumFilter.enumConstants
            val enumConstant = enumConstants.find { (it as Enum<*>).name == (if (all)"ALL" else "NOT") }

            val bookFilterInstance = bookFilter.getDeclaredConstructor().newInstance()
            val keywordConstructor = keywordFilter.getDeclaredConstructor(String::class.java)
            val keywordFilterInstance = keywordConstructor.newInstance("")
            refreshMethod.invoke(obj, enumConstant, bookFilterInstance, keywordFilterInstance)
        }

    /**
     * 将钱迹的账本同步给自动记账
     * @throws AutoAccountingException
     */
    suspend fun books() =
        withContext(Dispatchers.IO) {
            val list = XposedHelpers.callMethod(bookManager, "getAllBooks", true, 1) as List<*>
            val bookList = arrayListOf<BookModel>()

            /**
             * [
             *     {
             *         "bookId":-1,
             *         "cover":"http://res.qianjiapp.com/headerimages2/daniela-cuevas-t7YycgAoVSw-unsplash20_9.jpg!headerimages2",
             *         "createtimeInSec":0,
             *         "expired":0,
             *         "memberCount":1,
             *         "name":"日常账本",
             *         "sort":0,
             *         "type":-1,
             *         "updateTimeInSec":0,
             *         "userid":"",
             *         "visible":1
             *     }
             * ]
             */
            list.forEach { book ->
                if (bookClazz.isInstance(book)) {
                    val bookModel = BookModel()
                    // Get all fields of the Book class
                    val fields = bookClazz.declaredFields
                    for (field in fields) {
                        field.isAccessible = true
                        val value = field.get(book)
                        when (field.name) {
                            "name" -> bookModel.name = value as String
                            "cover" -> bookModel.icon = value as String
                            "bookId" -> {
                                val hashMap =
                                    withContext(Dispatchers.Main) {
                                        getCategoryList(value as Long)
                                    }
                                val arrayList = arrayListOf<CategoryModel>()
                                convertCategoryToModel(
                                    hashMap["list1"] as List<Any>,
                                    BillType.Expend,
                                ).let {
                                    arrayList.addAll(it)
                                }
                                convertCategoryToModel(
                                    hashMap["list2"] as List<Any>,
                                    BillType.Income,
                                ).let {
                                    arrayList.addAll(it)
                                }

                                bookModel.category = arrayList
                            }
                        }
                    }

                    // 同步完成账本后，获取对应账本对应的分类
                    // bookModel.category = categories()
                    bookList.add(bookModel)
                }
            }

            XposedBridge.log("同步账本信息:${Gson().toJson(bookList)}")

            AutoAccounting.setBooks(context, Gson().toJson(bookList))
        }

    /**
     * 将分类转为自动记账的分类模型
     */
    private fun convertCategoryToModel(
        list: List<*>,
        type: BillType,
    ): ArrayList<CategoryModel> {
        val categories = arrayListOf<CategoryModel>()
        list.forEach {
            if (it == null) return@forEach
            val category = it
            val model =
                CategoryModel(
                    type = type.value,
                )
            val fields = category::class.java.declaredFields
            for (field in fields) {
                field.isAccessible = true
                val value = field.get(category) ?: continue
                /**
                 * [
                 *     {
                 *         "bookId": -1,
                 *         "editable": 1,
                 *         "icon": "http://res3.qianjiapp.com/cateic_gongzi.png",
                 *         "id": 20001,
                 *         "level": 1,
                 *         "name": "工资",
                 *         "parentId": -1,
                 *         "sort": 0,
                 *         "type": 1,
                 *         "userId": "u10001"
                 *     },
                 *     {
                 *         "bookId": -1,
                 *         "editable": 1,
                 *         "icon": "http://res3.qianjiapp.com/cateic_shenghuofei.png",
                 *         "id": 20002,
                 *         "level": 1,
                 *         "name": "生活费",
                 *         "parentId": -1,
                 *         "sort": 0,
                 *         "type": 1,
                 *         "userId": "u10001"
                 *     },
                 *     {
                 *         "bookId": -1,
                 *         "editable": 1,
                 *         "icon": "http://res3.qianjiapp.com/cateic_hongbao.png",
                 *         "id": 20003,
                 *         "level": 1,
                 *         "name": "收红包",
                 *         "parentId": -1,
                 *         "sort": 0,
                 *         "type": 1,
                 *         "userId": "u10001"
                 *     },
                 *     {
                 *         "bookId": -1,
                 *         "editable": 1,
                 *         "icon": "http://res3.qianjiapp.com/cateic_waikuai.png",
                 *         "id": 20004,
                 *         "level": 1,
                 *         "name": "外快",
                 *         "parentId": -1,
                 *         "sort": 0,
                 *         "type": 1,
                 *         "userId": "u10001"
                 *     },
                 *     {
                 *         "bookId": -1,
                 *         "editable": 1,
                 *         "icon": "http://res3.qianjiapp.com/cateic_gupiao.png",
                 *         "id": 20005,
                 *         "level": 1,
                 *         "name": "股票基金",
                 *         "parentId": -1,
                 *         "sort": 0,
                 *         "type": 1,
                 *         "userId": "u10001"
                 *     },
                 *     {
                 *         "bookId": -1,
                 *         "editable": 0,
                 *         "icon": "http://res3.qianjiapp.com/cateic_other.png",
                 *         "id": 20006,
                 *         "level": 1,
                 *         "name": "其它",
                 *         "parentId": -1,
                 *         "sort": 0,
                 *         "type": 1,
                 *         "userId": "u10001"
                 *     }
                 * ]
                 */
                when (field.name) {
                    "name" -> model.name = value as String
                    "icon" -> model.icon = value as String
                    "id" -> model.id = (value as Long).toString()
                    "parentId" -> model.parent = (value as Long).toString()
                    "sort" -> model.sort = value as Int
                    "subList" -> {
                        val subList = value as List<*>
                        //    XposedBridge.log("子分类:${Gson().toJson(subList)}")
                        categories.addAll(convertCategoryToModel(subList, type))
                    }
                }
            }
            categories.add(model)
        }
        return categories
    }

    /**
     * 将钱迹的资产同步给自动记账
     * @throws AutoAccountingException
     */
    suspend fun assets() =
        withContext(Dispatchers.IO) {
            val accounts =
                withContext(Dispatchers.Main) {
                    getAssetsList()
                }
            val assets = arrayListOf<AssetsModel>()

            accounts.forEach {
                val asset = it!!
                val model = AssetsModel()
                // XposedBridge.log("账户信息:${Gson().toJson(asset)}")
                val fields = asset::class.java.declaredFields

                val stypeField = fields.filter { field -> field.name == "stype" }.getOrNull(0)
                val typeField = fields.filter { field -> field.name == "type" }.getOrNull(0)
                if (typeField == null || stypeField == null) return@forEach
                stypeField.isAccessible = true
                val stype = stypeField.get(asset) as Int

                typeField.isAccessible = true
                val type = typeField.get(asset) as Int

                //    public static final int SType_Debt = 51;
                //    public static final int SType_Debt_Wrapper = 61;
                //    public static final int SType_HuaBei = 22;
                //    public static final int SType_Loan = 52;
                //    public static final int SType_Loan_Wrapper = 62;
                //    public static final int SType_Money_Alipay = 13;
                //    public static final int SType_Money_Card = 12;
                //    public static final int SType_Money_WeiXin = 14;
                //    public static final int SType_Money_YEB = 103;

                //    public static final int Type_Credit = 2;
                //    public static final int Type_DebtLoan = 5;
                //    public static final int Type_DebtLoan_Wrapper = 6;
                //    public static final int Type_Invest = 4;
                //    public static final int Type_Money = 1;
                //    public static final int Type_Recharge = 3;
                model.type =
                    when (type) {
                        1 -> AssetType.CASH
                        2 -> AssetType.CREDIT
                        3 -> AssetType.RECHARGE
                        4 -> AssetType.INVEST
                        5 ->
                            when (stype) {
                                51 -> AssetType.DEBT_EXPAND
                                else -> AssetType.DEBT_INCOME
                            }

                        else -> AssetType.APP
                    }

                for (field in fields) {
                    field.isAccessible = true
                    val value = field.get(asset) ?: continue
                    when (field.name) {
                        "name" -> model.name = value as String
                        "icon" -> model.icon = value as String
                        "sort" -> model.sort = value as Int
                        "currency" -> model.currency = Currency.valueOf(value as String)
                        "loanInfo" -> model.extra = Gson().toJson(value)

                        "extra" -> {
                            if (model.extra.isEmpty() || model.extra == "{}") {
                                model.extra = Gson().toJson(value)
                            }
                        }
                    }
                }
                assets.add(model)
            }
            XposedBridge.log("同步账户信息:${Gson().toJson(assets)}")
            AutoAccounting.setAssets(context, Gson().toJson(assets))
        }

    /**
     * 将钱迹的账单同步给自动记账
     * @throws AutoAccountingException
     */
    suspend fun billsFromQianJi() =
        withContext(Dispatchers.IO) {
            val books = XposedHelpers.callMethod(bookManager, "getAllBooks", true, 1) as List<*>

            // 报销账单
            val bxList =
                withContext(Dispatchers.Main) {
                    getBaoXiaoList()
                }

            /**
             * {
             *     "_id": 10699,
             *     "assetid": 1613058959055,
             *     "billid": 1701309886298153304,
             *     "bookId": -1,
             *     "category": {
             *         "bookId": -1,
             *         "editable": 1,
             *         "icon": "http://res.qianjiapp.com/ic_cate2_dache.png",
             *         "id": 10753051,
             *         "level": 2,
             *         "name": "打车",
             *         "parentId": 10753046,
             *         "sort": 500,
             *         "type": 0,
             *         "userId": "200104405e109647c18e9"
             *     },
             *     "categoryId": 10753051,
             *     "createtimeInSec": 1701309886,
             *     "descinfo": "支付宝-余额宝",
             *     "fromact": "支付宝-余额宝",
             *     "fromid": -1,
             *     "importPackId": 0,
             *     "money": 28.23,
             *     "paytype": 0,
             *     "platform": 0,
             *     "remark": "高德打车 - 无备注信息",
             *     "status": 1,
             *     "targetid": -1,
             *     "timeInSec": 1701309880,
             *     "type": 5,
             *     "updateTimeInSec": 0,
             *     "userid": "200104405e109647c18e9"
             * }
             */
            val bx = Gson().toJson(convertBills(bxList, books))
            XposedBridge.log("同步报销账单:$bx")
            AutoAccounting.setBills(context, bx, BillType.ExpendReimbursement.name)
        }

    suspend fun billsFromAuto() =
        withContext(Dispatchers.IO) {
            runCatching {
                val bills = Gson().fromJson(AutoAccounting.getBills(context), List::class.java) as List<*>
                XposedBridge.log("同步自动记账账单:$bills")
                if (bills.isEmpty()) return@runCatching
                withContext(Dispatchers.Main) {
                    hooker.hookUtils.toast("正在从自动记账同步账单（${bills.size}）")
                }
                var index = 1
                bills.forEach {
                    withContext(Dispatchers.Main) {
                        hooker.hookUtils.toast("正在从自动记账同步账单（$index/${bills.size}）")
                    }
                    val gson = Gson()
                    val bill = gson.fromJson(gson.toJson(it), AutoBillModel::class.java)
                    // 构建钱迹账单
                    val uri = QianjiUri(bill, hooker.configSyncUtils.config).getUri()
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                    index++
                    XposedBridge.log("同步自动记账账单:$uri")
                    delay(3000L)
                }
                withContext(Dispatchers.Main) {
                    hooker.hookUtils.toast("账单同步完成！")
                }
            }
        }

    /**
     * 转换为自动记账需要的账单
     */
    private suspend fun convertBills(
        anyBills: List<*>,
        books: List<*>,
    ): List<BillModel> {
        val bills = arrayListOf<BillModel>()
        anyBills.forEach {
            val bill = BillModel()
            bill.type = BillType.ExpendReimbursement
            val fields = billClazz.declaredFields
            for (field in fields) {
                field.isAccessible = true
                val value = field.get(it)
                when (field.name) {
                    "money" -> bill.amount = value as Double
                    "billid" -> bill.id = (value as Long).toString()
                    "remark" -> bill.remark = (value as String?) ?: ""
                    "createtimeInSec" -> bill.time = (value as Long) * 1000
                    "fromact" -> bill.accountFrom = value as String
                    "descinfo" -> bill.accountTo = value as String
                    "bookId" -> {
                        books.forEach { book ->
                            if (bookClazz.isInstance(book)) {
                                val fieldInfo =
                                    bookClazz.declaredFields.filter { item -> item.name == "bookId" }
                                        .getOrNull(0)
                                fieldInfo?.apply {
                                    this.isAccessible = true
                                    bill.book = (this.get(book) as Long).toString()
                                }
                            }
                        }
                    }

                    "category" -> {
                        if (category.isInstance(value)) {
                            val fieldInfo =
                                category.declaredFields.filter { item -> item.name == "name" }
                                    .getOrNull(0)
                            fieldInfo?.apply {
                                this.isAccessible = true
                                bill.category = this.get(value) as String
                            }
                        }
                    }
                }
            }
            bills.add(bill)

            // 债务账单
        }
        return bills
    }
}
