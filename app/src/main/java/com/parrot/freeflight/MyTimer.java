package com.parrot.freeflight;

import android.os.Handler;

/**
 * Created by yizheng on 6/27/17.
 */

public class MyTimer extends Handler {
    private boolean isPaused;

    public synchronized void onPause(){
        isPaused=true;
    }

    public synchronized void onResume(){
        isPaused=false;
    }
}
