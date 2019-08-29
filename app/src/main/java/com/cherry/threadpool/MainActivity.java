package com.cherry.threadpool;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        for (int i = 0;i<21;i++){
            ThreadPool.getInstance().executor(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.e("TAG", "Thread Name" + Thread.currentThread().getName());
            });
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
              Log.e("TAG","---process your runnable --"+Thread.currentThread().getName());
            }
        };
        Worker worker = new MySubWorker(runnable);
        worker.start();

    }

}
