package com.mi.replacemethod

/**
 * create by niuxiaowei
 * date : 21-10-15
 **/
class InvokeType  {
   companion object{
       /**
        * INVOKE_STATIC调用静态方法，  INVOKE_VIRTUAL调用实例的非私有方法 ，INVOKE_NEW new一个实例
        */
       const val INVOKE_STATIC = "static"
       const val INVOKE_VIRTUAL = "ins"
       const val INVOKE_NEW = "new"
   }
}