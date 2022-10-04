package com.example.amisvp.helper;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AsyncTaskHelper {
    private final Executor executor = Executors.newSingleThreadExecutor(); // change according to your requirements
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface Callback<R> {
        void onComplete(R result);
    }

    public <R> void executeAsync(Callable<R> callable, Callback<R> callback) {
        executor.execute(() -> {
            R result = null;
            try {
                result = callable.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
            final R finalResult = result;
            handler.post(() -> {
                callback.onComplete(finalResult);
            });
        });
    }
}
