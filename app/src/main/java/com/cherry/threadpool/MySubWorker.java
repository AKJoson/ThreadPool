package com.cherry.threadpool;

import android.util.Log;

public class MySubWorker extends Worker{

    public MySubWorker(Runnable firstTask) {
        super(firstTask);
    }

    @Override
    protected void beforeRunTask(Thread thread, Runnable runnable) {
        super.beforeRunTask(thread, runnable);
        Log.e("TAG","--before process--"+Thread.currentThread().getName());
    }

    @Override
    protected void afterRunTask(Thread thread, Runnable runnable) {
        super.afterRunTask(thread, runnable);
        Log.e("TAG","--after process--"+Thread.currentThread().getName());
    }
}
