package com.appsuresolution.tensorflowimageclassifier;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ClassifierAsync {
    private final Handler handler;
    private final Context context;
    private final Executor executor;
    private final Classifier classifier;

    private Consumer<Void> onStarted;
    private Consumer<Void> onFinished;
    private Consumer<Throwable> onError;
    private Consumer<Map<String, Float>> onSuccess;

    public ClassifierAsync(@NotNull Context context) {
        this.context = context;
        this.handler = new Handler();
        this.executor = Executors.newSingleThreadExecutor();
        this.classifier = new Classifier(context);
        this.executor.execute(() -> {
            try {
                classifier.init();
            } catch (IOException e) {
                if (onError != null) {
                    onError.accept(e);
                }
            }
        });

    }

    public void setOnStarted(Consumer<Void> onStarted) {
        this.onStarted = onStarted;
    }

    public void setOnFinished(Consumer<Void> onFinished) {
        this.onFinished = onFinished;
    }

    public void setOnError(Consumer<Throwable> onError) {
        this.onError = onError;
    }

    public void setOnSuccess(Consumer<Map<String, Float>> onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void recognize(final Bitmap image) {
        if (onStarted != null) {
            handler.post(() -> onStarted.accept(null));
        }
        final HashMap<String, Float> classifierResults = classifier.process(image);
        if (onSuccess != null) {
            handler.post(() -> onSuccess.accept(classifierResults));
        }
        if (onFinished != null) {
            handler.post(() -> onFinished.accept(null));
        }
    }
}
