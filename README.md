# HotfixSophixDemo 阿里热修复之Sophix集成步骤
## 一、阿里Sophix
### 1.1 Sophix产品优势与不足 
HotFix总体来说最大的优势在于： 
* 补丁及时生效，不需要应用重启；  
* 补丁包同样采用差量技术，生成的PATCH体积小；  
* 对应用无侵入，几乎无性能损耗；  
* 两行代码，傻瓜式接入。  
不足：  
* 不支持新增字段，以及修改<init>方法，也不支持对资源的替换；  
* 由于厂商的自定义ROM，对少数机型暂不支持。  

[移动热修复用户指南](https://help.aliyun.com/document_detail/51434.html?spm=a2c4g.11174283.3.2.6ef530c3L5K4ax)  

### 1.2 Sophix与QQ空间超级补丁技术、微信Tinker的对比
参考：[Android热修复技术选型——三大流派解析](https://mp.weixin.qq.com/s/uY5N_PSny7_CHOgUA99UjA?spm=a2c4g.11186623.2.13.211b3a3ck8Ij07)
#### 1.2.1 QQ空间超级补丁技术
基于DEX分包方案，使用了多DEX加载的原理，大致过程：把BUG方法修复以后，放到一个单独的DEX里，插入到dexElements数组
的最前面，让虚拟机去加载修复完后的方法。
优势：  
* 没有合成整包（和微信Tinker比起来），产物比较小，比较灵活；  
* 可以实现类替换，兼容性高。（某些三星手机不起作用）  
不足：  
* 不支持及时生效，必须通过重启才能生效；  
* 为了实现修复这个过程，必须在应用中加入两个dex，dalvikhack.dex中只有一个类，对性能影响不大，但对于patch.dex来说
修复的类到了一定数量，就需要花不少的时间加载来启动应用；  
* 在ART模式下，如果类修改了结构，就会出现内存错乱的问题。为了解决这个问题，必须把所有相关的调用类、父类子类等等全部
加载的patch.dex中，导致补丁包异常的大，进一步增加应用启动加载时间，耗时更加严重。  
#### 1.2.2 微信Tinker
微信针对QQ空间超级补丁技术的不足提出了一个提供DEX差量包、整体替换DEX的方案。主要原理和QQ空间超级补丁技术基本相同，
区别在于不再将patch.dex增加到elements数组中，而是差量的方式给出patch.dex，然后将patch.dex与应用的classes.dex
合并，然后整体替换掉旧的DEX，达到修复的目的。  
优势：  
* 合成整包，不用在构造函数插入代码，防止verify，verify和opt在编译期间就已经完成，不会在运行期间进行；    
* 性能提高，兼容性和稳定性比较高；  
* 开发者透明，不需要对包进行额外处理。  
不足：  
* 与超级补丁一样不支持即时生效，必须通过重启应用的方式才能生效；  
* 需要给应用开启新的进程才能进行合并，并且很容易因为内存消耗等原因合并失败；  
* 合并时占用额外磁盘空间，对应多DEX的应用来说，如果修改了多个DEX文件，就需要下发多个patch.dex与对应的classes.dex
进行合并操作，次数这种情况会更严重，因此合并过程的失败率也更高。  
#### 1.2.3 阿里百川HotFix 
该方案提供了一种运行时在Native修改Field指针的方式，实现方法的替换，达到热修复及时生效，无需重启并且对应用无性能
消费的目的。
优势：  
* BUG修复的及时性；  
* 补丁包同样采用差量技术，生成的PATCH体积小；  
* 对应用无侵入，几乎无性能损耗。  
不足：  
* 不支持新增字段，以及修改<init>方法，也不支持对资源的替换；  
* 由于厂商的自定义ROM，对少数机型暂不支持。  
#### 1.2.4 总结
![image](https://github.com/tianyalu/HotfixSophixDemo/blob/master/show/compare.png)

## 二、Sophix集成步骤
官网的Android SKD稳健接入文档地址：[点击进入](https://help.aliyun.com/document_detail/51434.html?spm=a2c4g.11174283.3.2.6ef530c3L5K4ax)  
官方参考demo地址：[alicloud-android-demo](https://github.com/aliyun/alicloud-android-demo) （由于demo内容陈旧于文档，不建议参考）
### 2.1 创建产品与应用
在阿里官网，进入[移动研发平台](https://www.aliyun.com/product/emas?spm=a2c4g.11186623.2.12.49ee5cd7mdHyJC),
进入管理控制台，创建产品，成功后会生成一个`aliyun-emas-services.json`文件，这是一个配置文件，里面含有多个重要的key，
在后面的项目工程中要配置使用，十分重要！
### 2.2 创建项目
#### 2.2.1 app模块下的`build.gradle`文件
```groovy
apply plugin: 'com.android.application'

repositories {
    //阿里云仓库
    maven {
        url "http://maven.aliyun.com/nexus/content/repositories/releases"
    }
}
android {
    compileSdkVersion 26
    defaultConfig {
        applicationId "com.sty.hotfix.sophix"
        minSdkVersion 19
        targetSdkVersion 26
        versionCode 4
        versionName "1.3.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"

        //开启multidex
        multiDexEnabled true
    }

    //签名配置
    signingConfigs {
        release {
            try {
                storeFile file("./keystore/bugly_demo_keystore")
                storePassword "123456"
                keyAlias "key0"
                keyPassword "123456"
            } catch (ex) {

            }
        }

        debug {
            try {
                storeFile file("./keystore/bugly_demo_keystore")
                storePassword "123456"
                keyAlias "key0"
                keyPassword "123456"
            } catch (ex) {

            }
        }
    }

    //构建类型
    buildTypes {
        release {
            minifyEnabled true
            signingConfig signingConfigs.release
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
        debug {
            minifyEnabled false
            signingConfig signingConfigs.debug
            debuggable true
        }
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'com.android.support:appcompat-v7:26.1.0'
    implementation 'com.android.support.constraint:constraint-layout:1.1.3'

    implementation 'com.android.support:multidex:1.0.3'
    implementation 'com.aliyun.ams:alicloud-android-hotfix:3.2.12'
}
```

#### 2.2.2 `AndroidManifest.xml`文件
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.sty.hotfix.sophix">

    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <!-- 外部存储读权限，调试工具加载本地补丁需要 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

    <application
        android:name=".SophixStubApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!--对应 aliyun-emas-services.json 中的 hotfix.idSecret -->
        <!--<meta-data-->
            <!--android:name="com.taobao.android.hotfix.IDSECRET"-->
            <!--android:value="28219819-1" />-->
        <!--对应 aliyun-emas-services.json 中的 emas.appSecret -->
        <!--<meta-data-->
            <!--android:name="com.taobao.android.hotfix.APPSECRET"-->
            <!--android:value="2fc471c9c9c8bcfa78c4ce2eb1fc828b" />-->
        <!--对应 aliyun-emas-services.json 中的 hotfix.rsaSecret -->
        <!--<meta-data-->
            <!--android:name="com.taobao.android.hotfix.RSASECRET"-->
            <!--android:value="MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7fFUAatDx0X6v0xPVHvsF/GDPLdCCMRB5RLeBzPKUXaiz061BWXf3zW/30WKGU1hX262RS6bQ/AW9b8vpzfTddUZx1+okfEjF3ht1TJgmD89wlulmQIqp0vKCjPeWRcmU6vz+Q0+MreWuN/8LsSYb7M5m+Ic9wywAHGRRg2HMyTkl1vuYmS5uhO2ZtRWbbu6erKRruqQSn6whuZQtwRnOaeadTnKia+3dOHnWHpeC+MQZr1dNSO6njvI6Nw+IbbLs4+peD1iBGcOrXLGAnLNyISAn4pXpaSWDc4n2jpAIPnttgTMv56HGUqY8JmJBu1lDjfYRoIQrb3Sv7MIKVBa1AgMBAAECggEBAKlG5OhviSAM6rmNdwah5jvNVws4bl5eNmWHqbhEd3GF7uAOawvp3+uZTVMPdkmZztXkvp3FFGT0Z6pW2mHLVx7s2CHJtNokbQBaPah285jKBZ2+ZBWyiIA0ukktRg4kDJGsaaiY1aZLDGl0nY7M73+fG+zU0CbvpTaka989JHxkVgz2hOvQY315TLCTDhbk5byuYiIfVT3rrHaRQQTN6f6LVXw43VALh1K3tZqZnm9frxzfgRcRWFHm5tdHcm0WHV/2QuqxYMxktxcGBe5Lbri+QkCqsxkKAJFJe0sofSKh7UU+5AXo4pXyUgRNCnQZzZxb7uK2IPfAXYUIWUcH1DECgYEA6Yr5H3M7YXcuStG6RoqVkx+ZmGwM4ZqNbpaFdNCRQcEOe6mFJkggaR64eR3uBQIY6F6gyWqLFzISKDUHvQFcjUtnkGqZk4FLN7/fK9G8ypgx26wx6xbmV5zSHQ+qHIdq3kKVXqOSfKJ1BsHh9+uqQ8qRdhi4lVuan8PHG/LKv7sCgYEAzYOWmulYZ4NsQiOl4RtkeTP8QrvaKxNDSofN5pxFHhpQK0mPNuPV2A9xItGQHIKl0oEkMeVciRpcMx/TTT9Doj0UtrRLM0MZ7nVbuHBPNstgYWfABKxqtUc8+1ueaiUPSJ3dEC3tYRdicVRxCMe+zmO+7iUVHm2rPbxXW4IMBE8CgYBUC7hlshF/nOVi9aEEu+DiRm//I3trQTR+xNqYVGrB8BknNbV+Nly7l9upcrTyxnNyRIO2mIofudxA7fyv7yqAis9lcId+Rvxrvrgwy9UWT7k5pfl88gvJId2Xqch9Zdp2zB13JoSUJ97XpLSOhRgMN30rs+/RiFrX/hvMmg6O+wKBgCIopga3lE8YRA02OaMR14BhyKMa8CdKu1uO+QVEoiRfwUWAo4I/GlMgNfDQnkBu4rr7ZhW5BslvDTYeKxPi2Q4FUwUPMJSjqZK+BPHCERiNc0adZQgh1FavMY6h6tG/uSubuZuv1o3VwjIOE/+em7NYVurkVP9KwTSwsmBuXCr3AoGAXUo52cmajuq7aZFQrgmYFzMhwtrd5wj3x2xQb2iju3vK34gO9QXfMgYrjnyJ00T+5fhe93v91w9QPhFJbYAnkMu7M8we5LBiu6a9k9J0o4B20OUZpvFdAH/6o/bTA7W0GgiyLbOu6FBwQPaYvWSRBWSt+OkHv4CgccHb8WfALxE=" />-->
    </application>
</manifest>
```

#### 2.2.3 `MyApplication.java`文件
```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //做和自己业务逻辑相关的东西
        Log.i("sty", "MyApplication 处理自己业务逻辑的真正Application");
    }
}
```

#### 2.2.4 `SophixStubApplication.java`文件
```java
public class SophixStubApplication extends SophixApplication {
    private static final String TAG = SophixStubApplication.class.getSimpleName();

    //此处SophixEntry应指定真正的Application，并且保证RealApplicationStub类名不被混淆
    @Keep
    @SophixEntry(MyApplication.class)
    static class RealApplicationStub{}

    @Override
    public void onCreate() {
        super.onCreate();
        //setTags
        //该参数设置端上拉取补丁包时的标签，可支持条件更为丰富的灰度发布：同一版本号下，可以打两个或多个基线包，
        //这样就可以测试同一板号号下的同一个补丁了，两个环境互不影响。
        List<String> tags = new ArrayList<>();
        tags.add("test");
        //SophixManager.getInstance().setTags(tags); //该方法调用在queryAndLoadNewPatch()方法之前

        //queryAdnLoadNewPatch不可放在attachBaseContext中，否则无网络权限，建议放在后面任意时刻，如onCreate中
        //该方法主要用来查询服务器是否有新的可用补丁，SDK内部限制连续两次该方法调用不能短于3s，否则的话就会报code:19
        //的错误。如果查询可用的话，手先下载补丁到本地，然后：
        //  1.应用原本没有补丁，那么如果当前应用的补丁是热补丁，那么会立刻加载（不管是冷补丁还是热补丁），如果当前应用
        //的补丁是冷补丁，那么需要重启生效；
        //  2.应用已经存在一个补丁，请求发现有新补丁后，本次不受影响。并且在下次启动时补丁文件删除，下载并预加载新
        //补丁，在下下次启动时应用新补丁；（补丁在后台发布之后，并不会主动下行推送到客户端，需要手动调用该方法查询
        //后台补丁是否可用）
        //  3.只会下载补丁版本号比当前应用存在的补丁版本号高的补丁，比如当前应用已经下载了补丁版本号为5的补丁，那么
        //只有后台发布的补丁版本号>5时才会重新下载。
        //如果后台点击了“一键清除”补丁的话，这个方法将会返回code:18的状态码，此时本地补丁将会被强制清除，同时不清除
        //本地补丁版本号。
        SophixManager.getInstance().queryAndLoadNewPatch();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //如果需要使用MultiDex，需要在此处调用
        MultiDex.install(this);
        initSophix();
    }

    private void initSophix() {
        String appVersion = "0.0.0";
        //对应 aliyun-emas-services.json 中的 hotfix.idSecret
        String idSecret = "28219819-1";
        //对应 aliyun-emas-services.json 中的 emas.appSecret
        String appSecret = "2fc471c9c9c8bcfa78c4ce2eb1fc828b";
        //对应 aliyun-emas-services.json 中的 hotfix.rsaSecret
        String rsaSecret = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7fFUAatDx0X6v0xPVHvsF/GDPLdCCMRB5RLeBzPKUXaiz061BWXf3zW/30WKGU1hX262RS6bQ/AW9b8vpzfTddUZx1+okfEjF3ht1TJgmD89wlulmQIqp0vKCjPeWRcmU6vz+Q0+MreWuN/8LsSYb7M5m+Ic9wywAHGRRg2HMyTkl1vuYmS5uhO2ZtRWbbu6erKRruqQSn6whuZQtwRnOaeadTnKia+3dOHnWHpeC+MQZr1dNSO6njvI6Nw+IbbLs4+peD1iBGcOrXLGAnLNyISAn4pXpaSWDc4n2jpAIPnttgTMv56HGUqY8JmJBu1lDjfYRoIQrb3Sv7MIKVBa1AgMBAAECggEBAKlG5OhviSAM6rmNdwah5jvNVws4bl5eNmWHqbhEd3GF7uAOawvp3+uZTVMPdkmZztXkvp3FFGT0Z6pW2mHLVx7s2CHJtNokbQBaPah285jKBZ2+ZBWyiIA0ukktRg4kDJGsaaiY1aZLDGl0nY7M73+fG+zU0CbvpTaka989JHxkVgz2hOvQY315TLCTDhbk5byuYiIfVT3rrHaRQQTN6f6LVXw43VALh1K3tZqZnm9frxzfgRcRWFHm5tdHcm0WHV/2QuqxYMxktxcGBe5Lbri+QkCqsxkKAJFJe0sofSKh7UU+5AXo4pXyUgRNCnQZzZxb7uK2IPfAXYUIWUcH1DECgYEA6Yr5H3M7YXcuStG6RoqVkx+ZmGwM4ZqNbpaFdNCRQcEOe6mFJkggaR64eR3uBQIY6F6gyWqLFzISKDUHvQFcjUtnkGqZk4FLN7/fK9G8ypgx26wx6xbmV5zSHQ+qHIdq3kKVXqOSfKJ1BsHh9+uqQ8qRdhi4lVuan8PHG/LKv7sCgYEAzYOWmulYZ4NsQiOl4RtkeTP8QrvaKxNDSofN5pxFHhpQK0mPNuPV2A9xItGQHIKl0oEkMeVciRpcMx/TTT9Doj0UtrRLM0MZ7nVbuHBPNstgYWfABKxqtUc8+1ueaiUPSJ3dEC3tYRdicVRxCMe+zmO+7iUVHm2rPbxXW4IMBE8CgYBUC7hlshF/nOVi9aEEu+DiRm//I3trQTR+xNqYVGrB8BknNbV+Nly7l9upcrTyxnNyRIO2mIofudxA7fyv7yqAis9lcId+Rvxrvrgwy9UWT7k5pfl88gvJId2Xqch9Zdp2zB13JoSUJ97XpLSOhRgMN30rs+/RiFrX/hvMmg6O+wKBgCIopga3lE8YRA02OaMR14BhyKMa8CdKu1uO+QVEoiRfwUWAo4I/GlMgNfDQnkBu4rr7ZhW5BslvDTYeKxPi2Q4FUwUPMJSjqZK+BPHCERiNc0adZQgh1FavMY6h6tG/uSubuZuv1o3VwjIOE/+em7NYVurkVP9KwTSwsmBuXCr3AoGAXUo52cmajuq7aZFQrgmYFzMhwtrd5wj3x2xQb2iju3vK34gO9QXfMgYrjnyJ00T+5fhe93v91w9QPhFJbYAnkMu7M8we5LBiu6a9k9J0o4B20OUZpvFdAH/6o/bTA7W0GgiyLbOu6FBwQPaYvWSRBWSt+OkHv4CgccHb8WfALxE=";
        try {
            appVersion = this.getPackageManager()
                    .getPackageInfo(this.getPackageName(), 0)
                    .versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        final SophixManager instance = SophixManager.getInstance();
        instance.setContext(this)  //传入入库Application即可
                .setAppVersion(appVersion)  //版本号
                .setSecretMetaData(idSecret, appSecret, rsaSecret) //对应AndroidManifest中的设置的三个值（若不为null会覆盖AndroidManifest中的）
                .setEnableDebug(BuildConfig.DEBUG)  //是否调试模式，默认false，调试模式下会输出日志以及不进行补丁签名校验，正式发布时必须为false
                .setEnableFullLog()
                .setPatchLoadStatusStub(new PatchLoadStatusListener() { //设置patch加载状态监听器
                    /**
                     * 补丁加载状态会回调给该接口
                     * @param mode 无实际意义，为兼容老版本，默认始终为0
                     * @param code 补丁加载状态码，详情可参考：https://help.aliyun.com/document_detail/61082.html?spm=a2c4g.11186623.2.14.5dbeba6drQHvaN#h2-1-2-2
                     * @param info 补丁加载详细说明
                     * @param handlePatchVersion 当前处理的补丁版本号，0:无，-1:本地补丁，其它:后台补丁
                     */
                    @Override
                    public void onLoad(int mode, int code, String info, int handlePatchVersion) {
                        if(code == PatchStatus.CODE_LOAD_SUCCESS) {
                            //表明补丁加载成功
                            Log.i(TAG, "sophix load patch success!");
                        }else if(code == PatchStatus.CODE_LOAD_RELAUNCH) {
                            //表明新补丁生效需要重启，开发者可提醒用户或者强制重启；建议：可以监听用户进入后台事件，然后应用自杀
                            //如果需要在后台重启，建议此处用SharePreference保存状态
                            //可以在监听到CODE_LOAD_RELAUNCH后在合适的时机，调用此方法杀死进程（不可直接
                            //调用Process.killProcess(Process.myPid()）来杀死进程）
                            //instance.killProcessSafely();
                            Log.i(TAG, "sophix preload patch success. restart app to make effect.");
                        }else {
                            //其它错误信息，查看PatchStatus类说明
                        }
                    }
                }).initialize();
    }
}
```

#### 2.2.5 `proguard-rules.pro` 文件
```proguard
#基线包使用，生成mapping.txt
-printmapping mapping.txt
#生成的mapping.txt在app/build/outputs/mapping/release路径下，移动到/app路径下
#修复后的项目使用，保证混淆结果一致
#-applymapping mapping.txt
#hotfix
-keep class com.taobao.sophix.**{*;}
-keep class com.ta.utdid2.device.**{*;}
-dontwarn com.alibaba.sdk.android.utils.**
#防止inline
-dontoptimize

-keepclassmembers class com.sty.hotfix.sophix.MyApplication {
    public <init>();
}
# 如果不使用android.support.annotation.Keep则需加上此行
# -keep class com.sty.hotfix.sophix.SophixStubApplication$RealApplicationStub
```

### 2.3 生成补丁包
* Mac版本打包工具下载地址：[http://ams-hotfix-repo.oss-cn-shanghai.aliyuncs.com/SophixPatchTool_macos.zip](http://ams-hotfix-repo.oss-cn-shanghai.aliyuncs.com/SophixPatchTool_macos.zip)  
* Windows版本打包工具下载地址：[http://ams-hotfix-repo.oss-cn-shanghai.aliyuncs.com/SophixPatchTool_windows.zip](http://ams-hotfix-repo.oss-cn-shanghai.aliyuncs.com/SophixPatchTool_windows.zip)  

* 调试工具下载地址：[http://ams-hotfix-repo.oss-cn-shanghai.aliyuncs.com/hotfix_debug_tool-release.apk](http://ams-hotfix-repo.oss-cn-shanghai.aliyuncs.com/hotfix_debug_tool-release.apk)  

详情可参考官方文档：[生成补丁包步骤](https://help.aliyun.com/document_detail/93826.html?spm=a2c4g.11186623.4.1.20ed5b844LUcQt)  

**注意：**  
* 签名是必须要填写的，高级设置里面有一些参数，可视具体情况而定；  
* 产生的补丁包名称为：sophix-patch.jar，不可修改！  

### 2.4 上传补丁
在阿里控制台的“移动热修复”中进行“添加版本”、“上传补丁”等操作，详情可参考官方文档：[上传补丁](https://help.aliyun.com/document_detail/93806.html?spm=a2c4g.11186623.6.559.23415abdssgHwy)  

### 2.5 发布补丁
在“上传补丁”操作中经过“扫码内测”之后就可以发布补丁了，可以“灰度发布”，也可以“全量发布”，详情可参考官方文档：[发布补丁](https://help.aliyun.com/document_detail/93808.html?spm=a2c4g.11186623.6.560.6e135482lZ1n0n)  
**注意：**  
发布前要严格按照：扫码内测=>灰度发布=>全量发布 的流程进行，以保证补丁包能够正常在所有Android版本的机型上生效。  

