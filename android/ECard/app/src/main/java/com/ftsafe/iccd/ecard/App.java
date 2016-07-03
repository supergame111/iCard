package com.ftsafe.iccd.ecard;

import android.app.Application;
import android.os.Handler;

/**
 * Created by qingyuan on 16/7/3.
 */
public class App extends Application {

    private Handler mHandler;

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public Handler getHandler() {
        return mHandler;
    }

}
