package sux.tests.capturestartstop;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String[] PERMISSIONS = {Manifest.permission.CAMERA};
    public static final int PERMISSION_CODE = 0;

    //----------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e("F", "0");
        setContentView(R.layout.activity_main);

        Log.e("F", "0A");
        for (String permission : PERMISSIONS) {
            int permission_value = checkSelfPermission(permission);

            if (permission_value == PackageManager.PERMISSION_DENIED) {
                super.requestPermissions(PERMISSIONS, PERMISSION_CODE);
            }
            Log.e("F", "0B");
            step1();
        }
    }

    //----------------------------------------------------------------------------------------------

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e("F", "0C");
        step1();
    }

    //----------------------------------------------------------------------------------------------

    class SurfaceListener implements TextureView.SurfaceTextureListener {
        Surface nSurface;

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            nSurface = new Surface(texture);
            step2();
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
    }
    SurfaceListener mSurfaceListener = new SurfaceListener();

    public void step1() {
        Log.e("F", "1");
        TextureView textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(mSurfaceListener);
        setContentView(textureView);
    }

    //----------------------------------------------------------------------------------------------

    class CameraState extends CameraDevice.StateCallback {
        CameraDevice nCamera;
        CaptureRequest.Builder nBuilder;

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            nCamera = camera;
            nBuilder = null;
            try {nBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);}
            catch (CameraAccessException e) { }
            assert nBuilder != null;
            step3();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) { }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) { }
    }
    CameraState mCameraState = new CameraState();

    public void step2() {
        Log.e("F", "2");
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e("F", "2A");
        String[] cameraIds = null;
        try { cameraIds = cameraManager.getCameraIdList(); }
        catch (CameraAccessException e) { }
        Log.e("F", "2B");
        assert cameraIds != null;
        Log.e("F", "2C");
        try { cameraManager.openCamera(cameraIds[0], mCameraState, null); }
        catch (SecurityException e) { }
        catch (CameraAccessException e) { }
    }

    //----------------------------------------------------------------------------------------------

    class SessionState extends CameraCaptureSession.StateCallback {
        CameraCaptureSession nSession;

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            nSession = session;
            step4();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) { }
    };
    SessionState mSessionState = new SessionState();
    public void step3() {
        Log.e("F", "3");
        List<Surface> surfaceList = new ArrayList<>();
        surfaceList.add(mSurfaceListener.nSurface);
        mCameraState.nBuilder.addTarget(mSurfaceListener.nSurface);;
        Log.e("F", "3A");
        try { mCameraState.nCamera.createCaptureSession(surfaceList, mSessionState, null); }
        catch (CameraAccessException e) { }
    }

    //----------------------------------------------------------------------------------------------

    class SessionCapture extends CameraCaptureSession.CaptureCallback {
        final int LIMIT = 10;
        int nCount = 0;
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session,
                                     @NonNull CaptureRequest request,
                                     long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            nCount += 1;
            if (nCount >= LIMIT) {
                nCount = 0;
                try { session.stopRepeating(); }
                catch (CameraAccessException e) { }
            }
        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session,
                                               int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
            step5();
        }
    };
    SessionCapture mSessionCapture = new SessionCapture();
    public void step4() {
        Log.e("F", "4");
        CaptureRequest request = mCameraState.nBuilder.build();
        Log.e("F", "4A");
        try { mSessionState.nSession.setRepeatingRequest(request, mSessionCapture, null); }
        catch (CameraAccessException e) { }
    }

    public void step5() {
        Log.e("F", "5");
        CaptureRequest request = mCameraState.nBuilder.build();
        Log.e("F", "5A");
        try { mSessionState.nSession.setRepeatingRequest(request, mSessionCapture, null); }
        catch (CameraAccessException e) { }
    }
}

