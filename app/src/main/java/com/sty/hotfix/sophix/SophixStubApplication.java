package com.sty.hotfix.sophix;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.Keep;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.taobao.sophix.PatchStatus;
import com.taobao.sophix.SophixApplication;
import com.taobao.sophix.SophixEntry;
import com.taobao.sophix.SophixManager;
import com.taobao.sophix.listener.PatchLoadStatusListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tian on 2019/12/16.
 */

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
