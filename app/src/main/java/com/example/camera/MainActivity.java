package com.example.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private TextureView mTextureView;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;
    // ????????? ????????? ?????? ??????
    private Size mPreviewSize;
    private StreamConfigurationMap map;
   // private MediaScanner mMediaScanner; //?????? ?????? ??? ????????? ????????? ?????? ??????????????? ???????????? ??????????????? ????????? ????????? ??????

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //?????? ?????? ??? ????????? ???????????? ???????????? ???????????? ?????????.
        //mMediaScanner = MediaScanner.getInstance(getApplicationContext());

        //?????? ??? ?????? ??????
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ????????? ????????? ????????? ????????????.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    200);
        } else {
            // ????????? ?????? ???????????? layout ??? ????????????.
            initLayout();
        }

    }

    //?????? ?????? ?????? layout ??????
    private void initLayout() {
        setContentView(R.layout.activity_main);
        mTextureView = findViewById(R.id.preview);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

////        ??????????????? ??????!
//        public void scheduleJob(View v){
//            ComponentName componentName = new ComponentName(this, JobService.class);
//            //????????????
//            JobInfo info = new JobInfo.Builder(     , componentName)
//                    .setPersisted(true)
//                    .setPeriodic(1)
//                    .build();
//
//            JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
//
//            int resultCode = jobScheduler.schedule(info);
//
//
//        try {
//            takePicture();
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }

//         Thread => takePicture()
        new Thread(
                () -> {
                    while(true) {
                        try {
                            Thread.sleep(1000);
                            takePicture();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).start();

//        //textureView ??? ???????????? ???????????? ??????
//            mTextureView.setOnClickListener(v -> {
//                try {
//                    takePicture();
//                } catch (CameraAccessException e) {
//                    e.printStackTrace();
//                }
//            });

//        take Picture

        }
       /* int totalTimes = 10;
        double times = 0.5;

        while (times <= totalTimes) {

            mTextureView.setOnClickListener(v -> {
                try {
                    takePicture();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            });
            times++;
        }*/


    //??????????????? ?????? callback
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 200 && grantResults.length > 0) {
            boolean permissionGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    // ???????????? ????????? ????????????.
                    permissionGranted = false;
                    break;
                }
            }

            if (permissionGranted) {
                // ?????? ????????? ????????? ????????? layout ??? ????????????.
                initLayout();
            } else {
                Toast.makeText(this,
                        "????????? ???????????? ?????? ????????? ???????????????",
                        Toast.LENGTH_LONG).show();
                finish();
            }

        }
    }
    //TextureView ????????? ????????? ??????????????? ???????????? ????????? SurfaceTextureListener ????????? onSerfaceTextureAvailable()???????????? ??????
    // textureView ??? ????????? ??????????????? ???????????? onSurfaceTextureAvailable()??????

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    // cameraManager ???????????? ?????????
                    openCamera(width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            }; //TextureView.SurfaceTextureListener

//CameraManager ??????
    //openCamera(width,height)?????????
    //1.CameraManager ??????
    //2.????????? ?????? ?????? ??????
    //3.openCamera()?????? -> CameraDevice ?????? ??????

    private void openCamera(int width, int height) {
        // CameraManager ?????? ??????
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // default ???????????? ????????????.
            String cameraId = manager.getCameraIdList()[1];

            // ????????? ?????? ????????????
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            int level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            Range<Integer>[] fps = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Log.d("maximum frame rate is :", fps[fps.length - 1] + "hardware level = " + level);

            // StreamConfigurationMap ???????????? ???????????? ?????? ?????? ????????? ????????????.
            map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // ??????????????? textureview ?????????????????? ?????? <- ????????? ??? ?????? ??????????????? ????????????.
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
            Range<Integer>[] fpsForVideo = map.getHighSpeedVideoFpsRanges();
            Log.e("for video :", fpsForVideo[fpsForVideo.length - 1] + " preview Size width:" + mPreviewSize.getWidth() + ", height" + height);

            // ????????? ??????
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // ????????? ????????? ????????? ????????????.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        200);
            } else {
                // CameraDevice ??????
                manager.openCamera(cameraId, mStateCallback, null);
            }

        } catch (CameraAccessException e) {
            Log.e("openCamera() :", "????????? ??????????????? ???????????? ????????? ????????????.");
        }
    }

    //manager.openCamera(cameraId, mStateCallback , null ) ?????? mStateCallback ?????????????????? ???????????? ????????? ??????

    private CameraDevice.StateCallback mStateCallback
            = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            // CameraDevice ?????? ??????
            cameraDevice = camera;
            // CaptureRequest.Builder ????????? CaptureSession ?????? ???????????? ???????????? ????????? ???????????????.
//            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    //startPreview() > CaptureRequest.Builder ?????? ??? CaptureSession ?????? ??????

    private void startPreview() {
        if (cameraDevice == null ||
                !mTextureView.isAvailable() ||
                mPreviewSize == null) {
            Log.e("startPreview() fail", "return ");
            return;
        }

        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        Surface surface = new Surface(texture);
        try {
            mPreviewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            Log.e("fail", "CaptureRequest ?????? ?????? ??????");
            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(surface);

        try {
            cameraDevice.createCaptureSession(Arrays.asList(surface),  // / ????????????????????? ????????? ????????? surface ?????? ??????
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    }, null);
        } catch (CameraAccessException e) {
            Log.e("fail", "CaptureSession ?????? ?????? ??????");
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) {
            return;
        }
        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());
        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void takePicture() throws CameraAccessException {
        Size[] jpegSizes = null;
        if (map != null) jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
        int width = 640;
        int height = 480;
        if (jpegSizes != null && 0 < jpegSizes.length) {
            width = jpegSizes[0].getWidth();
            height = jpegSizes[0].getHeight();
        }
        final ImageReader imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG,1);

        List<Surface> outputSurfaces = new ArrayList<>(2);
        outputSurfaces.add(imageReader.getSurface());
        outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));

        // ImageCapture ??? ?????? CaptureRequest.Builder ??????
        final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(imageReader.getSurface());

        // ?????? ????????? api ??? ??? ?????? ??????X
        // ???????????? ???????????? ????????? ????????? ?????? ???????????? ??????????????? 3A??? ???????????? ??????
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

//        int rotation = getWindowManager().getDefaultDisplay().getRotation();
//        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
//        String imageFileName = "Capture_" + timeStamp + "_";


//        File[] storageDir = getExternalFilesDirs(Environment.DIRECTORY_PICTURES);
//        File image = File.createTempFile(imageFileName,".jpeg",storageDir);
//        String filename = timeStamp + " / "+ imageFileName;
//    final File file = new File(Environment.getExternalStorageDirectory()+"/DCIM","pic.jpg"); -???? ??????
        File file = new File(Environment.getExternalStorageDirectory()+"/DCIM", timeStamp + ".jpeg");

        // ???????????? ????????? ??? ???????????? ????????????.
        ImageReader.OnImageAvailableListener readerListener =
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = null;
                        try {
                            image = imageReader.acquireLatestImage();

                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);

                            save(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (image != null) {
                                image.close();
                                reader.close();
                            }
                        }
                    }

//                    ????????? ????????????(Bitmap -> Bitmap)
                    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
                        int width = bm.getWidth();
                        int height = bm.getHeight();
                        float scaleWidth = ((float) newWidth) / width;
                        float scaleHeight = ((float) newHeight) / height;
                        // CREATE A MATRIX FOR THE MANIPULATION
                        Matrix matrix = new Matrix();
                        // RESIZE THE BIT MAP
                        matrix.postScale(scaleWidth, scaleHeight);

                        // "RECREATE" THE NEW BITMAP
                        Bitmap resizedBitmap = Bitmap.createBitmap(
                                bm, 0, 0, width, height, matrix, true);
                        bm.recycle();
                        return resizedBitmap;
                    }

                    private void method(String img_Data, String timeStamp) {
                        OkHttpClient client = new OkHttpClient().newBuilder().build();
                        MediaType mediaType = MediaType.parse("application/json;ty=4");
                        RequestBody body = RequestBody.create( "{\n    \"m2m:cin\": {\n        \"con\": \""+ img_Data +"\",\n        \"rn\" : \""+timeStamp+"\"\n    }\n}",mediaType);
                        Request request = new Request.Builder()
                                .url("http://203.253.128.177:7579/Mobius/SW_Hackaton/test_img")
                                .method("POST", body)
                                .addHeader("Accept", "application/json")
                                .addHeader("X-M2M-RI", "12345")
                                .addHeader("X-M2M-Origin", "SOrigin")
                                .addHeader("Content-Type", "application/json;ty=4")
                                .build();

                        try {
                            Response response = client.newCall(request).execute();
                            System.out.println(response.body().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    private void save(byte[] bytes) throws IOException {

//                        TO DO. Mobius upload

                        OutputStream output = null;

                        try {
                            output = new FileOutputStream(file);
                            output.write(bytes);

                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            Bitmap bitmap1 = getResizedBitmap(bitmap, 192, 192);

//                            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

//                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                            bitmap.compress(Bitmap.CompressFormat.JPEG,90,outputStream );
//
//                            byte[] imagebyte = outputStream.toByteArray();
//                            System.out.println(imagebyte);
//                            Log.d("image ","image"+imagebyte);

//                            ?????? ?????? ?????????
//                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                            bitmap1.compress(Bitmap.CompressFormat.JPEG, 70, baos);
//                            byte[] bytes1 = baos.toByteArray();
//                            String temp = Base64.encodeToString(bytes1, Base64.DEFAULT);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap1.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            byte[] bytes1 = baos.toByteArray();
                            String temp = Base64.getEncoder().encodeToString(bytes1);

                            System.out.println(temp);
//                            Mobius ?????????

                            new Thread(
                                    () -> method(temp, timeStamp)
                            ).start();

                            //bitmap

                        } finally {
                            if (null != output) {
                                output.close();
//                               mMediaScanner.mediaScanning(file+"/"+filename+".jpeg");*
//                               ????????? ?????? ??????

                                if (file.exists() && file.isFile() && file.length() > 0) {
                                    byte[] bt = new byte[(int) file.length()];
                                    FileInputStream fis = null;

                                    try {
                                        fis = new FileInputStream(file);
                                        fis.read(bt);


                                    } catch (Exception e) {
                                        throw e;

                                    } finally {
                                        try {
                                            if (fis != null) {
                                                fis.close();
                                            }
                                        } catch (IOException e) {
                                        } catch (Exception e) {
                                        }
                                    }


                                }
                            }
                        }

                    }


                };
        //????????? ?????? ?????? -> ?????? ????????? X, ????????? ????????? O
        //???????????? ????????? ?????? ?????? ????????????
        HandlerThread thread = new HandlerThread("CameraPicture");
        thread.start();
        final Handler backgroundHandler = new Handler(thread.getLooper());

        // ImageReader ??? ImageReader.OnImageAvailableListener ????????? ?????? ?????????????????? ?????? ??????
        imageReader.setOnImageAvailableListener(readerListener, backgroundHandler);

        // ?????? ???????????? ????????? ?????? ???????????? ?????????
        final CameraCaptureSession.CaptureCallback captureCallback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(CameraCaptureSession session,
                                                  CaptureRequest request, TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        Toast.makeText(MainActivity.this, "saved:"+file, Toast.LENGTH_SHORT).show();
                        Log.d("saved : ","file" +file);
//                        // ???????????? ??????????????? ???????????? ?????? ??????????????? ????????????.
//                        startPreview();
                    }
                };
            /*
            ?????? ???????????? ??????????????? ???????????? CameraCaptureSession ??? ????????????.
            ?????? ???????????? ?????? ????????? ???????????? ??????
            */
        try {
            CaptureRequest.Builder finalCaptureBuilder = captureBuilder;
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(finalCaptureBuilder.build(), captureCallback, backgroundHandler);

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, backgroundHandler);
        } catch (CameraAccessException cameraAccessException) {
            Log.e("fail","takePicture() createCaptureRequest fail");
            cameraAccessException.printStackTrace();
        }

    }
}
