package com.appsuresolution.tensorflowimageclassifier;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.DequantizeOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class Classifier implements Closeable {
    private final Context mContext;
    private final List<String> labels;
    private final static String MODEL_NAME = "efficientnet-lite0-int8.tflite";
    private final static String LABELS_NAME = "labels_without_background.txt";
    private Interpreter interpreter;
    private FileInputStream inputStream;
    private FileChannel fileChannel;
    private AssetFileDescriptor assetFileDescriptor;

    public Classifier(@NotNull Context argContext) {
        this.mContext = argContext;
        this.interpreter = null;
        this.labels = new LinkedList<>();
    }

    public void init() throws IOException {
        initLabels();
        initInterpreter();
    }

    private void initLabels() throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(mContext.getAssets().open(LABELS_NAME)))) {
            while (bufferedReader.ready()) {
                labels.add(bufferedReader.readLine());
            }
        }
    }

    private void initInterpreter() throws IOException {
        assetFileDescriptor = mContext.getAssets().openFd(MODEL_NAME);
        final FileDescriptor fileDescriptor = assetFileDescriptor.getFileDescriptor();
        inputStream = new FileInputStream(fileDescriptor);
        fileChannel = inputStream.getChannel();
        final ByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFileDescriptor.getStartOffset(), assetFileDescriptor.getDeclaredLength());
        interpreter = new Interpreter(buffer);
    }

    public HashMap<String, Float> process(@NotNull final Bitmap bitmap) {
        final TensorImage image = new TensorImage(DataType.UINT8);
        final int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        final ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(size, size))
                .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(new NormalizeOp(0.0f, 1.0f))
                .build();
        image.load(bitmap);
        final TensorImage scaled = imageProcessor.process(image);

        final TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, labels.size()}, DataType.UINT8);
        interpreter.run(scaled.getBuffer(), probabilityBuffer.getBuffer());

        final TensorProcessor probabilityProcessor =
                new TensorProcessor.Builder().add(new NormalizeOp(0, 255)).build();
        final TensorLabel tensorLabels = new TensorLabel(this.labels,
                probabilityProcessor.process(probabilityBuffer));

        final List<Map.Entry<String, Float>> toBeSorted = new LinkedList<>(tensorLabels.getMapWithFloatValue().entrySet());
        Collections.sort(toBeSorted, (o1, o2) -> Float.compare(o1.getValue(), o2.getValue()));
        final HashMap<String, Float> sorted = new LinkedHashMap<>();
        int index = toBeSorted.size() - 1;
        for (int i = 0; i < 3; i++) {
            final Map.Entry<String, Float> current = toBeSorted.get(index - i);
            if (current.getValue() < 0.3) {
                break;
            }
            sorted.put(current.getKey(), current.getValue());
        }
        return sorted;
    }

    @Override
    public void close() throws IOException {
        assetFileDescriptor.close();
        fileChannel.close();
        inputStream.close();
        interpreter.close();
    }


}
