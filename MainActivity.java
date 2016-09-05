package com.example.student.camera2;

import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.view.TextureView;
import android.widget.Button;

import java.util.jar.Manifest;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    TextureView.SurfaceTextureListener textureListener;
    TextureView txvOutput;
    Button btnTakePicture;
    String cameraId ;
    Size cameraSize;
    CameraDevice cameraDevice;
    CameraDevice.StateCallback stateCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txvOutput = (TextureView) findViewById(R.id.txvOutput);
        btnTakePicture = (Button) findViewById(R.id.btnTakePicture);
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
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map !=null) {
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

}
