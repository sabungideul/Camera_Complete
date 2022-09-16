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
    // 카메라 설정에 관한 변수
    private Size mPreviewSize;
    private StreamConfigurationMap map;
   // private MediaScanner mMediaScanner; //사진 저장 시 갤러리 폴더에 바로 반영사항을 업데이트 시켜주려면 미디어 스캐닝 필요

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //사진 저장 후 미디어 스캐닝을 돌려줘야 갤러리에 반영됨.
        //mMediaScanner = MediaScanner.getInstance(getApplicationContext());

        //실행 중 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 권한을 요청한다.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    200);
        } else {
            // 권한이 있을 경우에만 layout 을 전개한다.
            initLayout();
        }

    }

    //권한 있을 경우 layout 정개
    private void initLayout() {
        setContentView(R.layout.activity_main);
        mTextureView = findViewById(R.id.preview);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

////        스케줄러에 넣기!
//        public void scheduleJob(View v){
//            ComponentName componentName = new ComponentName(this, JobService.class);
//            //작업설정
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

//        //textureView 를 클릭하면 이미지가 저장
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


    //권한요청에 관한 callback
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 200 && grantResults.length > 0) {
            boolean permissionGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    // 사용자가 권한을 거절했다.
                    permissionGranted = false;
                    break;
                }
            }

            if (permissionGranted) {
                // 권한 요청을 수락한 경우에 layout 을 전개한다.
                initLayout();
            } else {
                Toast.makeText(this,
                        "권한을 수락해야 어플 이용이 가능합니다",
                        Toast.LENGTH_LONG).show();
                finish();
            }

        }
    }
    //TextureView 객체가 화면에 정상적으로 나타나면 등록한 SurfaceTextureListener 객체의 onSerfaceTextureAvailable()메소드가 호출
    // textureView 가 화면에 정상적으로 출력되면 onSurfaceTextureAvailable()호출

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    // cameraManager 생성하는 메소드
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

//CameraManager 생성
    //openCamera(width,height)메소드
    //1.CameraManager 생성
    //2.카메라 관한 정보 얻기
    //3.openCamera()호출 -> CameraDevice 객체 생성

    private void openCamera(int width, int height) {
        // CameraManager 객체 생성
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // default 카메라를 선택한다.
            String cameraId = manager.getCameraIdList()[1];

            // 카메라 특성 알아보기
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            int level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            Range<Integer>[] fps = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Log.d("maximum frame rate is :", fps[fps.length - 1] + "hardware level = " + level);

            // StreamConfigurationMap 객체에는 카메라의 각종 지원 정보가 담겨있다.
            map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // 미리보기용 textureview 화면크기용을 설정 <- 제공할 수 있는 최대크기를 가리킨다.
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
            Range<Integer>[] fpsForVideo = map.getHighSpeedVideoFpsRanges();
            Log.e("for video :", fpsForVideo[fpsForVideo.length - 1] + " preview Size width:" + mPreviewSize.getWidth() + ", height" + height);

            // 권한에 대한
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // 권한이 없으면 권한을 요청한다.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        200);
            } else {
                // CameraDevice 생성
                manager.openCamera(cameraId, mStateCallback, null);
            }

        } catch (CameraAccessException e) {
            Log.e("openCamera() :", "카메라 디바이스에 정상적인 접근이 안됩니다.");
        }
    }

    //manager.openCamera(cameraId, mStateCallback , null ) 에서 mStateCallback 콜백메소드를 구현하여 객체를 생성

    private CameraDevice.StateCallback mStateCallback
            = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            // CameraDevice 객체 생성
            cameraDevice = camera;
            // CaptureRequest.Builder 객체와 CaptureSession 객체 생성하여 미리보기 화면을 실행시킨다.
//            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    //startPreview() > CaptureRequest.Builder 객체 와 CaptureSession 객체 생성

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
            Log.e("fail", "CaptureRequest 객체 생성 실패");
            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(surface);

        try {
            cameraDevice.createCaptureSession(Arrays.asList(surface),  // / 미리보기용으로 위에서 생성한 surface 객체 사용
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
            Log.e("fail", "CaptureSession 객체 생성 실패");
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

        // ImageCapture 를 위한 CaptureRequest.Builder 객체
        final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(imageReader.getSurface());

        // 이전 카메라 api 는 이 기능 지원X
        // 이미지를 캡처하는 순간에 제대로 사진 이미지가 나타나도록 3A를 자동으로 설정
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

//        int rotation = getWindowManager().getDefaultDisplay().getRotation();
//        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
//        String imageFileName = "Capture_" + timeStamp + "_";


//        File[] storageDir = getExternalFilesDirs(Environment.DIRECTORY_PICTURES);
//        File image = File.createTempFile(imageFileName,".jpeg",storageDir);
//        String filename = timeStamp + " / "+ imageFileName;
//    final File file = new File(Environment.getExternalStorageDirectory()+"/DCIM","pic.jpg"); -?얘 안됨
        File file = new File(Environment.getExternalStorageDirectory()+"/DCIM", timeStamp + ".jpeg");

        // 이미지를 캡처할 때 자동으로 호출된다.
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

//                    이미지 리사이즈(Bitmap -> Bitmap)
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

//                            내가 잠깐 지운거
//                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                            bitmap1.compress(Bitmap.CompressFormat.JPEG, 70, baos);
//                            byte[] bytes1 = baos.toByteArray();
//                            String temp = Base64.encodeToString(bytes1, Base64.DEFAULT);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap1.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            byte[] bytes1 = baos.toByteArray();
                            String temp = Base64.getEncoder().encodeToString(bytes1);

                            System.out.println(temp);
//                            Mobius 올리기

                            new Thread(
                                    () -> method(temp, timeStamp)
                            ).start();

                            //bitmap

                        } finally {
                            if (null != output) {
                                output.close();
//                               mMediaScanner.mediaScanning(file+"/"+filename+".jpeg");*
//                               미디어 스캔 돌림

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
        //이미지 캡처 작업 -> 메인 스레드 X, 스레드 핸들러 O
        //모비우스 올리는 작업 역시 스레드로
        HandlerThread thread = new HandlerThread("CameraPicture");
        thread.start();
        final Handler backgroundHandler = new Handler(thread.getLooper());

        // ImageReader 와 ImageReader.OnImageAvailableListener 객체를 서로 연결시켜주기 위해 설정
        imageReader.setOnImageAvailableListener(readerListener, backgroundHandler);

        // 사진 이미지를 캡처한 이후 호출되는 메소드
        final CameraCaptureSession.CaptureCallback captureCallback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(CameraCaptureSession session,
                                                  CaptureRequest request, TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        Toast.makeText(MainActivity.this, "saved:"+file, Toast.LENGTH_SHORT).show();
                        Log.d("saved : ","file" +file);
//                        // 이미지가 성공적으로 캡처되면 다시 미리보기를 수행한다.
//                        startPreview();
                    }
                };
            /*
            사진 이미지를 캡처하는데 사용하는 CameraCaptureSession 을 생성한다.
            이미 존재하면 기존 세션은 자동으로 종료
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
