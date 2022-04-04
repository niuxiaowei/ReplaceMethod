


# ReplaceMethod(对调用的方法进行替换的工具)

[![](https://jitpack.io/v/niuxiaowei/replacemethod.svg)](https://jitpack.io/#niuxiaowei/replacemethod)


*ReplaceMethod*:  **在代码编译阶段，根据收集的配置信息，利用ASM对字节码进行替换，以达到对调用的方法进行替换的工具**  (您不需要学习怎么写gradle插件，不需要学习ASM非常复杂的语法)

### 为什么要做这个工具

1. 治理项目中的线程问题
背景：由于我做的项目历史非常的悠久并且非常的庞大复杂，项目中的线程没有一个统一的管理方式并且野线程（没有名字的线程）到处飞。
于是想着使用ASM在编译过程中对所有new Thread的地方进行替换到某个方法中，在这个方法中来统一处理 统一管理
2. 在做 [轻量级LayoutInspector工具](https://github.com/niuxiaowei/LayoutInspector) 时候， 需要定位view被inflate的位置信息（哪个类的哪个方法哪行）以及view的点击事件的位置信息。想到的办法也是同上面（利用ASM在编译过程中替换字节码来实现）

于是我就想不能每次遇到 **对方法替换的时候** 就要写重复的写gradle插件，并且在写ASM相关的替换代码，那我为啥不写一个这样的工具呢（并且ASM相关的api真的很复杂），并且这个工具是可以做很多事情的。

### 用它能做什么

下面列了几个例子：

1. 对view.SetOnclickListener方法进行替换。比如：
   
   代码中的所有的**view.setOnClickListener**方法最终被下面的方法替换
   
   ```
   public static void setOnClickListener(View view, View.OnClickListener clickListener, Object[] objects) {
        view.setOnClickListener(new ClickListenerWrapper(clickListener,objects));
    }
   
    public static class ClickListenerWrapper implements View.OnClickListener {
        private View.OnClickListener listener;
        private Object[] params;
   
        public ClickListenerWrapper(View.OnClickListener listener,Object[] objects) {
            this.listener = listener;
            params = objects;
        }
   
        @Override
        public void onClick(View v) {
            String className = params[0]+"";
            String classSimpleName = className.substring(className.lastIndexOf(".") + 1);
            Log.i(TAG, "click info: (" + classSimpleName + ".java:" + params[3] + ")" + " or (" + classSimpleName + ".kt:" + params[3] + ")"+"   view:"+v+"  clickListener:"+listener);
            if (listener != null) {
                listener.onClick(v);
            }
        }
    }
   ```
   
   上面代码最终的效果是：在view被点击的时候，会打印当前setOnClickListener的具体代码处（类名，方法，行数）
2. 对各种new Thread() 进行治理，如下：
   
   把代码中的甚至是第三方库中的所有new Thread()的代码统一都转入下面的方法中生成线程
   
   ```
   public static Thread createThread() {
         //使用统一的创建线程的方法重新生成Thread,
   }
   ```
3. **排查修复隐私问题(众所周知现在隐私问题国家管控的非常严格)**，可以对涉及隐私的方法调用进行替换,  如下：
   
   获取手机mac的代码如下：
   
   ```
   WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
   WifiInfo wi = wm.getConnectionInfo();
    wi.getMacAddress()
   ```
   
   可以对**项目中所有的wi.getMacAddress**方法替换到下面方法中,在这个方法中就可以写自己的逻辑了:
   
   ```
   public static String getMacAddress(WifiInfo wi) {
         // 增加自己的逻辑，如添加log信息
         String result = wi.getMacAddress()
         return result;
   }
   ```
4. 对第三方jar或aar的方法进行替换，比如对第三方jar中的Log进行拦截，把需要的关键的log信息存储下来，或者修复第三方的bug
5. 大家还可以根据自己的需要来做其他更有趣的事情

### 实现原理

在说实现原理之前，先看下使用**ReplaceMethod**替换方法的例子

**对Activity的setContentView(int)方法进行替换**例子

![原先代码](/pictures/1.png)

![替换后的class](/pictures/2.png)

![最终调用的方法](/pictures/3.png)

上面例子展示了对Activity的setContentView(int)方法替换，

1. 会在当前的类中生成一个私有的静态的方法（当前的类实例及setContentView的参数作为参数）
2. 判断当前类是否是Activity的子类，是的话则调用**ReplaceMethodDemo.setContentView(Activity, int)** 方法， 至此setContentView(int)方法被替换为ReplaceMethodDemo.setContentView(Activity, int)
3. 若当前类不是Activity的子类，则还是执行之前的逻辑

##### 原理

看了上面的替换结果，我想大家冒出来的第一个问题是：替换不应该是 xxx.invokeAMethod -----> 被替换为yyy.invokeAMethod  这么简单吗？  为啥要生成私有的静态方法这个啰嗦的不在？关于这个问题在下面给与答复。

**方法替换的本质**就是:  xxx.invokeAMethod -----> yyy.invokeAMethod

围绕本质 ，实现主要做三件事情：

**1 收集替换信息**
收集替换信息主要是在**.gradle文件中进行配置（为啥没有采用在txt文本文件中进行配置的主要原因是，配置项目确实很多，在文本文件中配置起来非常麻烦，出现错误难以定位问题），具体的配置介绍会在后面介绍
收集需要替换的方法信息，在编译过程中通过ASM，查找到替换的方法后，插入替换者的信息字节码。

**2 定位替换方法**
利用ASM，根据收集到的信息，去定位具体的方法，定位的时候主要对比：**方法的owner（所属类），方法是静态的还是实例，方法的名称，方法描述符**。在定位**确定的方法**,比如：静态方法调用 **StaticClass.invokeStaticMethod()** （并且StaticClass的父类中没有定义invokeStaticMethod这个方法）的时候，是非常的简单的，在定位**调用的是父类的静态/非静态方法**是不能正确定位的，如上面例子  对**Activity**类的setContentView(int)方法替换，在ChildActivity（Activity子类）调用setContentView(int)方法，这时候的**owner**是ChildActivity ，它和Activity肯定是不一样的，这时候就会导致定位不到，但是ChildActivity中调用的setContentView(int)确实是需要替换的方法，那因此针对这种情况需要特殊处理，处理方法如下：

1. 在当前类中生成一个私有的静态方法，它的参数有(**当前类作为第一个参数**，替换方法的参数。静态/实例方法的参数是不一样的），它的返回值与替换方法保持一致
2. 在该静态方法中加入一些判断逻辑，判断第一个参数是否是替换方法owner的子类，是的话进行替换，不是则保存原先逻辑
3. 把调用替换方法的地方替换为调用  生成的私有静态方法

这样，就可以在运行期间来进行检测定位逻辑和替换逻辑了

**3 替换**
定位到替换方法后，利用ASM插入对应的字节码，主要分为几种情况：

1. 对于**确定的方法** 的方法，直接替换为目标类的方法名（目标类指yyy）,替换后的效果:xxx.invokeAMethod -----> yyy.invokeAMethod
2. 对于new对象的方法，直接替换为目标类的静态方法（目标类指yyy，静态方法的参数与构造函数参数一致，返回值为new对象对应的类），替换后的效果:  new MyClass( int ,int ) ------->  yyy.createMyClass( int, int)
3. 对于调用的是父类的静态/非静态方法，利用ASM在当前类中插入 私有静态方法（见 **2 定位替换方法**），替换后效果：xxx.invokeAMethod -----> 当前类.generateStaticMethod ------> yyy.invokeAMethod

### 接入（参考代码中的例子）

**1.工程的gradle文件**

```
buildscript {
    repositories {
                maven { url 'https://jitpack.io' }
       
    }
    dependencies {
        classpath "com.github.niuxiaowei:ReplaceMethod:1.0.4"
    }
}
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

**2.app的gradle文件**

```
apply plugin: 'ReplaceMethodPlugin'


replaceMethod{
    open true
    openLog true
    logFilters "IInterfaceTest"
    replaceByMethods{
        register {
            replace {
                invokeType "ins"
                className "android.view.LayoutInflater"
                methodName "inflate"
                desc "(int,android.view.ViewGroup)android.view.View"
            }
            by {
                className = "com.mi.replacemethod.ReplaceMethodDemo"
                methodName = "inflate"
                addExtraParams = true
            }
        }
   }
}
```

**3.泛型的配置**
比如配置List，如下面的配置方法都可以
```
register {
            replace {
                invokeType "ins"
                className "android.app.ActivityManager"
                methodName "getRunningTasks"
                desc "(int)java.util.List"
            }
            by {
                className = "com.mi.replacemethod.ReplaceMethodDemo"
            }
}
```
or
```
register {
            replace {
                invokeType "ins"
                className "android.app.ActivityManager"
                methodName "getRunningTasks"
                desc "(int)java.util.List<android.app.ActivityManager\$RunningTaskInfo>"
            }
            by {
                className = "com.mi.replacemethod.ReplaceMethodDemo"
            }
}
```


在**replaceMethod**中进行配置，可以参考代码中的例子。

**配置项介绍**
**open**: true：替换功能打开，  false：替换功能关闭
**openLog** : true: 编译过程中的log打开， 否则关闭 （日志建议不要打开，否则影响编译速度）
**logFilters**: 配合openLog使用，只有在openLog为true的情况下才有效。不配置则会把所有日志打印出来，配置后只显示配置的日志,可以配置多个,用","分割，如：    logFilters "IInterfaceTest","Main", "AA"
**replaceByMethods**：注册多个替换方法

**register**: 注册一对**replace** **by**。 可以这样理解register：**replace**中的方法被**by**方法替换

**replace**: 配置需要替换的方法，它的属性有：

1. **invokeType**，代表方法类型：静态的，实例，构造方法，它的值有：**static**（静态方法），**ins**（非私有实例方法），**new**（构造方法）
2. **className**，方法所属的类名， 配置内部类时候必须使用"\\\$", 如
   
   ```
   className "(android.view.View\$OnClickListener)"
   ```
3. **methodName**，方法的名称
4. **desc**，方法描述符配置，格式： (paramType, paramType2,...) returnType。
   paramType： 基本数据类型直接用基本数据类型，否则使用类的全路径，多个param直接用 **","** 分割
   returnType:    同上，代表返回类型，返回类型为void，可以不用配置
   若方法没有参数并且返回类型为void，则可不用配置该项
5. **releaseEnable**: 在buildType为release的时候 替换功能 是否有用。 true：代表该条替换在release进行替换，默认值false
6. **ignoreOverideStaticMethod**：针对**子类中调用父类定义的非私有静态方法**情况，默认值为false，如下面的例子：
   
   ```
   register {
            replace {
                invokeType "static"
                className "android.view.View"
                methodName "inflate"
                desc "(android.content.Context,int,android.view.ViewGroup)android.view.View"
                ignoreOverideStaticMethod true
            }
            by {
                className = "com.mi.replacemethod.ReplaceMethodDemo"
                methodName = "inflate"
                addExtraParams  true
            }
        }
   ```
   
   对View的inflate方法替换，若该值为true，则会忽略子类中重新定义的相同的inflate方法，而直接进行替换。
7. **replacePackages**：对哪些package进行替换，不配置则对所有的包进行替换，可以配置多个，如：replacePackages "com.mi","com.niu"

**by**:  配置替换**replace**的方法信息, 它的属性有：

1. **className**: 类名， 配置内部类时候必须使用"\\\$", 如
   
   ```
   className "(android.view.View\$OnClickListener)"
   ```
2. **methodName**：方法名，必须是**public类型的静态方法**, 若与**replace**的方法同名，则可不用配置
3. **addExtraParams**: 是否需要额外数据， true需要，**若配置为true，则在方法的参数中必须有一个Object[] 类型的参数，并且必须是最后一个参数，否则在运行过程中会奔溃**,   **Object[]** 信息有：object[0] 调用**replace方法**的类全路径名称,  object[1] 调用**replace方法**的方法名称， object[2] 调用**replace方法**的方法描述符合，object[3] 调用**replace方法**的行信息

### 还未实现功能

**不能对私有方法进行替换**
**不能对在子类中调用父类定义的静态方法进行替换**，比如：对在View子类的静态方法中调用View的inflate方法进行替换

```
public class MyView extends View{
  
      private static void init(Context context, int resource, ViewGroup root){
               inflate(contet, resource,  root);
     }

}
```

**我的公众号**
![我的微信公众号](https://user-images.githubusercontent.com/3078303/148639094-a57cf897-eec6-4d79-a724-42b36e742b96.jpg)

