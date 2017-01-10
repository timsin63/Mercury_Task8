package com.example.user.task_8;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import java.lang.reflect.Array;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{


    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray ORIENTATIONS_FRONT = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);

        ORIENTATIONS_FRONT.append(Surface.ROTATION_0, 270);
        ORIENTATIONS_FRONT.append(Surface.ROTATION_90, 0);
        ORIENTATIONS_FRONT.append(Surface.ROTATION_180, 90);
        ORIENTATIONS_FRONT.append(Surface.ROTATION_270, 180);
    }

    private static final String TAG = "MAIN_ACTIVITY";
    private static final String CAMERA_STATE_TAG = "STATE_PREF";
    private static final String FLASH_STATE_TAG = "FLASH_STATE_PREF";
    private static final int REQUEST_CODE_CAMERA = 350;

    private static final int CAMERA_STATE_BACK = 0;
    private static final int CAMERA_STATE_FRONT = 1;

    CameraDevice.StateCallback cameraStateCallback;
    CameraDevice camera;
    CameraManager cameraManager;
    String[] cameraIdsList;
    CameraCaptureSession captureSession;
    ImageReader imageReader;
    SurfaceView surfaceView, transparentView;
    SurfaceHolder surfaceHolder, transparentHolder;
    SharedPreferences preferences;
    CaptureRequest.Builder builder;
    ImageButton flashButton;

    Surface cameraSurface = null;
    int cameraState = 1;

    boolean isFlashActivated = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        transparentView = (SurfaceView) findViewById(R.id.transparent_view);

        surfaceHolder = surfaceView.getHolder();

        transparentHolder = transparentView.getHolder();
        surfaceHolder.addCallback(this);

        transparentHolder.setFormat(PixelFormat.TRANSLUCENT);
        transparentView.setZOrderMediaOverlay(true);

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

                if (cameraSurface != null) {
                    try {
                        configureCamera();
                    } catch (CameraAccessException e) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                }
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

        //Swiping
        if (cameraIdsList.length == 1){
            swipeButton.setEnabled(false);
            swipeButton.setVisibility(View.INVISIBLE);
        } else {
            swipeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (cameraState == CAMERA_STATE_BACK) {
                        cameraState = CAMERA_STATE_FRONT;
                    } else {
                        cameraState = CAMERA_STATE_BACK;
                    }
                    saveCameraState(cameraState, isFlashActivated);
                    openCam(cameraState);
                }
            });
        }


        isFlashActivated = getPreferences(MODE_PRIVATE).getBoolean(FLASH_STATE_TAG, false);
        //Flash
        flashButton = (ImageButton) findViewById(R.id.flash_button);

//        if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
//            flashButton.setEnabled(false);
//            flashButton.setVisibility(View.INVISIBLE);
//            isFlashActivated = false;
//        } else {
            if (isFlashActivated) {
                flashButton.setBackgroundResource(R.drawable.no_flash);
            } else {
                flashButton.setBackgroundResource(R.drawable.flash);
            }
            flashButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isFlashActivated) {
                        isFlashActivated = false;
                        flashButton.setBackgroundResource(R.drawable.flash);
                    } else {
                        isFlashActivated = true;
                        flashButton.setBackgroundResource(R.drawable.no_flash);
                    }
                    saveCameraState(cameraState, isFlashActivated);
                }
            });
  //      }

        //Capture
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

                    if (cameraState == CAMERA_STATE_FRONT){
                        builder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS_FRONT.get(rotation));
                    } else {
                        builder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
                    }

                    if (isFlashActivated){
                        builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                    } else {
                        builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                    }

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

    }

    private void openCam(int cameraId){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        }

        try {
            if (camera != null) {
                camera.close();
            }
            cameraManager.openCamera(cameraIdsList[cameraId], cameraStateCallback, null);
            Log.d(TAG, "openCam");
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (SecurityException e){
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void configureCamera() throws CameraAccessException {
        Log.d(TAG, "configure camera");

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
                        builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

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



        //TODO MANUAL FOCUS
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                final float pointX = motionEvent.getX();
                final float pointY = motionEvent.getY();

                Rect rect = new Rect((int) pointX - 100, (int) pointY - 100, (int) pointX + 100, (int) pointY + 100);

                MeteringRectangle meteringRectangle = new MeteringRectangle(rect, 1000);
                MeteringRectangle[] meteringRectangleArr = {meteringRectangle};

                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
                builder.set(CaptureRequest.CONTROL_AF_REGIONS, meteringRectangleArr);
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_MACRO);
                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
                builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);

                try {
                    captureSession.setRepeatingRequest(builder.build(), null, null);
                } catch (CameraAccessException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }


                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(getResources().getColor(R.color.white));
                final Canvas canvas = transparentHolder.lockCanvas();

                canvas.drawColor(getResources().getColor(R.color.transparent), PorterDuff.Mode.CLEAR);
                canvas.drawCircle(pointX, pointY, 100, paint);

                canvas.drawRect(rect, paint);
                transparentHolder.unlockCanvasAndPost(canvas);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        transparentHolder.lockCanvas();
                        canvas.drawColor(getResources().getColor(R.color.transparent), PorterDuff.Mode.CLEAR);
                        transparentHolder.unlockCanvasAndPost(canvas);
                    }
                }, 1000);

                return false;
            }
        });

    }

    private void saveCameraState(int cameraState, boolean isFlashActivated){
        preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(CAMERA_STATE_TAG, cameraState);
        editor.putBoolean(FLASH_STATE_TAG, isFlashActivated);
        Log.d("ISFLASH_SAVING", String.valueOf(isFlashActivated));
        editor.commit();
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
        if (cameraSurface != null) {
            cameraSurface.release();
        }
        if (captureSession != null) {
            captureSession.close();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();

        preferences = getPreferences(MODE_PRIVATE);
        cameraState = preferences.getInt(CAMERA_STATE_TAG, 0);

        Log.d("ISFLASH", String.valueOf(isFlashActivated));
        openCam(cameraState);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveCameraState(cameraState, isFlashActivated);
    }



}
