package com.cherry.threadpool;

import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * First: add the core thread process
 * Second: add the Queue
 * Last: add the maxCore thread process
 * Final: throw Exception
 */

public class ThreadPool {

    private ThreadPool() {
    }

    public static ThreadPool getInstance() {
        return ThreadHolderInstance.mInstance;
    }

    public static class ThreadHolderInstance {
        public static ThreadPool mInstance = new ThreadPool();
    }

    private static final BlockingQueue<Runnable> mPoolWorkQueue = new LinkedBlockingQueue<>(10);

    public static int a = 0;


    private static final ThreadFactory mThreadFactory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "create new thread" + (++a));
        }
    };

    /**
     * 一共可以有4个核心，10-4 = 6个非核心同时运行。
     */

    Executor executor = Executors.newScheduledThreadPool(4,mThreadFactory);
//    Executor executor = new ThreadPoolExecutor(4,
//            10,
//            60,
//            TimeUnit.SECONDS,
//            mPoolWorkQueue,
//            mThreadFactory, new RejectedExecutionHandler() {
//        @Override
//        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
//            Log.e("TAG", "---超出");
//        }
//    });

    public void executor(Runnable runnable) {
        executor.execute(runnable);
    }

    public static class Builder{

        public Builder(){

        }
        public Builder set

    }
}
