package com.example.user.task_8;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{


    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final String TAG = "MAIN_ACTIVITY";
    private static final int REQUEST_CODE_CAMERA = 350;

    CameraDevice.StateCallback cameraStateCallback;
    CameraDevice camera;
    CameraManager cameraManager;
    String[] cameraIdsList;
    CameraCaptureSession captureSession;
    ImageReader imageReader;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;

    Surface cameraSurface = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        surfaceView = (SurfaceView) findViewById(R.id.surface_view);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            cameraIdsList = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException");
        }

        cameraStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice cameraDevice) {
                camera = cameraDevice;

                Log.d(TAG, "camera: on Opened");
            }

            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                Log.e(TAG, "camera: onDisconnected");
            }

            @Override
            public void onError(CameraDevice cameraDevice, int i) {
                Log.e(TAG, "camera: onError " + i);

            }
        };


        ImageButton swipeButton = (ImageButton) findViewById(R.id.swipe_cam);

        swipeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                camera.close();
                cameraSurface.release();

                openCam(1);

            }
        });


        final ImageButton captureButton = (ImageButton) findViewById(R.id.capture_btn);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "capture clicked");


                try {
                    CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    builder.addTarget(imageReader.getSurface());
                    CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);
                            Log.d(TAG, "CAPTURED! YEAH");

                            Image image = imageReader.acquireLatestImage();
                            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[byteBuffer.capacity()];
                            byteBuffer.get(bytes);
                            Intent intent = new Intent(MainActivity.this, PreviewActivity.class);
                            intent.putExtra(PreviewActivity.IMAGE_TAG, bytes);

                            startActivity(intent);
                        }
                    };

                    int rotation = getWindowManager().getDefaultDisplay().getRotation();
                    builder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

                    captureSession.capture(builder.build(), captureCallback, null);
                } catch (CameraAccessException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        openCam(0);


    }

    private void openCam(int cameraId){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        }

        try {
            cameraManager.openCamera(cameraIdsList[cameraId], cameraStateCallback, null);
            Log.d(TAG, "openCam");
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (SecurityException e){
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void configureCamera() throws CameraAccessException {

        imageReader = ImageReader.newInstance(1280, 1024, ImageFormat.JPEG, 1);
        List<Surface> list = new ArrayList<>();
        list.add(cameraSurface);
        list.add(imageReader.getSurface());

            camera.createCaptureSession(list, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    captureSession = cameraCaptureSession;
                    Log.d(TAG, "captureSession onConfigured");

                    try {
                        CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        builder.addTarget(cameraSurface);
                        captureSession.setRepeatingRequest(builder.build(), null, null);

                    } catch (CameraAccessException e) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "CaptureSessionConfigure failed");
                }
            }, null);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA){
            boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        finish();
        startActivity(getIntent());
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        cameraSurface = surfaceHolder.getSurface();
        Log.d(TAG, "surface created");
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        cameraSurface = surfaceHolder.getSurface();

        try {
            configureCamera();
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        Log.d(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        Log.d(TAG, "surface destroyed");
    }

    @Override
    protected void onStop() {
        super.onStop();
        cameraSurface.release();
        captureSession.close();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();


    }
}
