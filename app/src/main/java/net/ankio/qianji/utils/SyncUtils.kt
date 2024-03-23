package net.ankio.qianji.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.ankio.auto.sdk.AutoAccounting
import net.ankio.common.constant.BillType
import net.ankio.common.model.BookModel
import net.ankio.common.model.CategoryModel
import net.ankio.qianji.api.Hooker
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import kotlin.coroutines.resume


/**
 * 用于将钱迹的数据同步给自动记账
 */
class SyncUtils(val context: Context,val classLoader: ClassLoader, private val hooker: Hooker) {


    lateinit var bookManager :Any
    lateinit var bookClazz :Class<*>
    lateinit var proxyClazz:Class<*>
    lateinit var cateInitPresenterImplClazz:Class<*>
    lateinit var category: Class<*>
    fun init(){
        //初始化账本信息
        val clazzName = hooker.clazz["BookManager"]
        bookManager = XposedHelpers.callStaticMethod(classLoader.loadClass(clazzName),"getInstance")
        bookClazz = classLoader.loadClass("com.mutangtech.qianji.data.model.Book")
        //初始化分类信息
        cateInitPresenterImplClazz = classLoader.loadClass("com.mutangtech.qianji.bill.add.category.CateInitPresenterImpl")
        val onGetCategoryList = hooker.clazz["onGetCategoryList"]
        proxyClazz = classLoader.loadClass(onGetCategoryList)

        category = classLoader.loadClass("com.mutangtech.qianji.data.model.Category")
    }

    private suspend fun getCategoryList(bookId: Long): HashMap<String,Any> = suspendCancellableCoroutine { continuation ->
        // 在一个新的线程中执行需要Looper的操作
        Thread {
            Looper.prepare()

            val handler = InvocationHandler { _, method, args ->
                if (method.name == "onGetCategoryList") {
                    val list1 = args[0]
                    val list2 = args[1]
                    // 在主线程中恢复协程
                    Handler(Looper.getMainLooper()).post {
                        continuation.resume(hashMapOf("list1" to list1, "list2" to list2))
                    }
                }
                null
            }
            val proxyInstance = Proxy.newProxyInstance(classLoader, arrayOf(proxyClazz), handler)
            val constructor = cateInitPresenterImplClazz.getDeclaredConstructor(proxyClazz)
            val obj = constructor.newInstance(proxyInstance)
            val loadCategoryListMethod = cateInitPresenterImplClazz.getMethod("loadCategoryList", Long::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
            loadCategoryListMethod.invoke(obj, bookId, true)

            Looper.loop()
        }.start()

        continuation.invokeOnCancellation {
            // 清理资源的代码
        }

    }

    suspend fun books() = withContext(Dispatchers.IO){
        val list = XposedHelpers.callMethod(bookManager,"getAllBooks",true,1) as List<*>
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
                   when(field.name){
                       "name" -> bookModel.name = value as String
                       "cover" -> bookModel.icon = value as String
                       "bookId" -> {
                          val hashMap = getCategoryList(value as Long)
                           val arrayList = arrayListOf<CategoryModel>()
                           convertCategoryToModel(hashMap["list1"] as List<Any>,BillType.Expend).let {
                               arrayList.addAll(it)
                           }
                           convertCategoryToModel(hashMap["list2"] as List<Any>,BillType.Income).let {
                               arrayList.addAll(it)
                           }

                           bookModel.category = arrayList
                       }
                   }
               }



               //同步完成账本后，获取对应账本对应的分类
               // bookModel.category = categories()
               bookList.add(bookModel)
           }

       }


        XposedBridge.log("同步账本信息:${Gson().toJson(bookList)}")

       AutoAccounting.setBooks(context,Gson().toJson(bookList))
    }

    private fun convertCategoryToModel(list:List<Any>,type: BillType): ArrayList<CategoryModel> {
        val categories = arrayListOf<CategoryModel>()
        list.forEach {
            val category = it
            val model = CategoryModel(
                type = type.value
            )
            val fields = category::class.java.declaredFields
            for (field in fields) {
                field.isAccessible = true
                val value = field.get(category)
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
                when(field.name){
                    "name" -> model.name = value as String
                    "icon" -> model.icon = value as String
                    "id" -> model.id = (value as Long).toString()
                    "parentId" -> model.parent = (value as Long).toString()
                    "sort" -> model.sort = value  as Int
                }
            }
            categories.add(model)
        }
        return categories
    }
}