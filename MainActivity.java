package com.example.student.camera2;

import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String TAG = "TEST_CAMERA";
    TextureView.SurfaceTextureListener textureListener;
    TextureView txvOutput;
    Button btnTakePicture;
    String cameraId ;
    Size cameraSize;
    CameraDevice cameraDevice;
    CameraDevice.StateCallback stateCallback;
    CaptureRequest.Builder captureRequestBuilder;
    CameraCaptureSession cameraCaptureSession;
    HandlerThread handlerThread;
    Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txvOutput = (TextureView) findViewById(R.id.txvOutput);
        btnTakePicture = (Button) findViewById(R.id.btnTakePicture);
        btnTakePicture.setOnClickListener(this);

        textureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCamera();
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
        };

         stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                cameraDevice = camera;
                createCameraPreview();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {

            }

            @Override
            public void onError(CameraDevice camera, int error) {

            }
        };

        if(txvOutput != null) {
            txvOutput.setSurfaceTextureListener(textureListener);
        }
    }

    private void createCameraPreview() {
        SurfaceTexture texture = txvOutput.getSurfaceTexture();
        if (texture == null) {
            return;
        }
        texture.setDefaultBufferSize(cameraSize.getWidth(), cameraSize.getHeight());
        Surface surface = new Surface(texture);
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(
                    Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            if (cameraDevice == null) {
                                return;
                            }
                            cameraCaptureSession = session;
                            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {

                        }
                    },
                    handler);

            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, handler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) finish();

        }
    }



    private void openCamera() {
        CameraManager manager = (CameraManager)getSystemService(CAMERA_SERVICE);

        try {
            cameraId = manager.getCameraIdList()[0];

            Log.d( TAG, cameraId.toString());

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map != null) {
                cameraSize = map.getOutputSizes(SurfaceTexture.class )[0];
            }

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CAMERA_PERMISSION);
                return;

            }

            manager.openCamera(cameraId, stateCallback, null );

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }



    }

    @Override
    protected void onResume() {
        super.onResume();
        handlerThread = new HandlerThread("Camera");
        handler = new Handler(handlerThread.getLooper());
        if (txvOutput.isAvailable()) {
            openCamera();

        } else {
            txvOutput.setSurfaceTextureListener(textureListener);
        }

    }


    @Override
    public void onClick(View v) {
        takePicture();

    }

    private void takePicture() {
        int width = 640;
        int height = 480;
        int rotation = 0;
        final File file;

        if (cameraDevice == null) return;

        CameraManager manager = (CameraManager)getSystemService(CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;

            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }

            if ( jpegSizes != null && jpegSizes.length > 0 ) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation);

            file = new File(Environment.getExternalStorageDirectory() + "/picture.jpeg");

            reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);

                    try {

                        OutputStream outputStream = new FileOutputStream(file);
                        outputStream.write(bytes);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }, handler);
            List<Surface> outputSurfaces = new ArrayList<Surface>();
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(txvOutput.getSurfaceTexture()));
            final CameraCaptureSession.CaptureCallback captureListner = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                }
            };


            cameraDevice.createCaptureSession( outputSurfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            //TODO
                                session.capture(captureBuilder.build(), captureListner, handler);

                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {

                        }
                    }


            );


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
