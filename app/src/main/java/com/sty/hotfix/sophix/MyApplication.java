package com.sty.hotfix.sophix;

import android.app.Application;
import android.util.Log;

/**
 * Created by tian on 2019/12/16.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //做和自己业务逻辑相关的东西
        Log.i("sty", "MyApplication 处理自己业务逻辑的真正Application");
    }
}
