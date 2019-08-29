package com.cherry.threadpool;

public class Worker implements java.lang.Runnable {
    private Thread thread;
    private Runnable firstTask;

    public Worker(Runnable firstTask){
        this.thread  = new Thread(this,"Thread 1"); //这个this ,蛮有意思的。
        this.firstTask = firstTask;
    }
    public void start(){
        if (thread!=null)
            thread.start();
    }

    @Override
    public void run() {
        runWorker(this);
    }

    private void runWorker(Worker worker) {
        beforeRunTask( worker.thread,worker.firstTask);
        worker.firstTask.run();
        afterRunTask(worker.thread,worker.firstTask);
    }

    protected void beforeRunTask(Thread thread,Runnable runnable){};

    protected void afterRunTask(Thread thread,Runnable runnable){};
}

