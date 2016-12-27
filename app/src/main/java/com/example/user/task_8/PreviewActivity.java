package com.example.user.task_8;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class PreviewActivity extends AppCompatActivity {

    public static final String TAG = "PREVIEW_ACTIVITY";
    public static final String IMAGE_TAG = "IMAGE_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_preview);

        ImageView preview = (ImageView) findViewById(R.id.image_preview);

        byte[] bytes = getIntent().getByteArrayExtra(IMAGE_TAG);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        preview.setImageBitmap(bitmap);
    }
}
