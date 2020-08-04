package com.appsuresolution.tensorflowimageclassifier;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_IMAGE_REQUEST = 999;
    private TextView mTextViewChooseImage;
    private TextView mTextViewStatus;
    private ImageView imageView;
    private ClassifierAsync mClassifier;
    private String mainDescription;
    private boolean isBusy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextViewChooseImage = findViewById(R.id.mTextViewChoose);
        mTextViewStatus = findViewById(R.id.mTextViewStatus);
        mTextViewStatus.setVisibility(View.GONE);
        imageView = findViewById(R.id.imageView);
        mTextViewChooseImage.setOnClickListener(this);
        mClassifier = new ClassifierAsync(this);
        mainDescription = getString(R.string.choose_image_from_gallery);
        mClassifier.setOnError(this::onError);
        mClassifier.setOnStarted(this::onStarted);
        mClassifier.setOnFinished(this::onFinished);
        mClassifier.setOnSuccess(this::onSuccess);
        isBusy = true;
    }

    private void onSuccess(Map<String, Float> stringFloatMap) {
        mTextViewStatus.setVisibility(View.VISIBLE);
        if(stringFloatMap == null || stringFloatMap.keySet().size() == 0){
            mTextViewStatus.setText("No object recognized.");
            return;
        }
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        stringBuilder.append("Object recognition results");
        stringBuilder.append("\n");
        for (final Map.Entry<String, Float> entry: stringFloatMap.entrySet()) {
            stringBuilder.append(entry.getKey() + "\t: "+entry.getValue());
            stringBuilder.append("\n");
        }
        mTextViewStatus.setText(stringBuilder.toString());

    }

    private void onFinished(Void aVoid) {

    }

    private void onStarted(Void aVoid) {
        mTextViewStatus.setVisibility(View.GONE);
    }

    private void onError(Throwable throwable) {
        mTextViewStatus.setVisibility(View.GONE);
        Toast.makeText(this,throwable.toString(),Toast.LENGTH_LONG).show();
    }

    private void pickImage() {
        final Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, mainDescription), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri filePath = data.getData();
            final Bitmap img;
            try {
                img = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                if (img != null) {
                    imageView.setImageBitmap(img);
                    mClassifier.recognize(img);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onClick(View v) {
        pickImage();
    }
}