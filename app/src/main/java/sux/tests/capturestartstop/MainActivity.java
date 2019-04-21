package sux.tests.capturestartstop;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    class ImageReaderListener implements ImageReader.OnImageAvailableListener {
        Surface nSurface;
        int nImageFormat = ImageFormat.RAW_SENSOR;

        ImageReaderListener() { }

        public void setup(CameraCharacteristics cameraCharacteristics) {
            class SizeCompare implements Comparator<Size> {
                public int compare(Size o1, Size o2) {
                    return -1 * Integer.compare(o1.getHeight()*o1.getWidth(), o2.getWidth()*o2.getHeight());
                }
            }

            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizeArray = streamConfigurationMap.getOutputSizes(nImageFormat);
            List<Size> sizeList = new ArrayList<>();
            Collections.addAll(sizeList, sizeArray);
            Collections.sort(sizeList, new SizeCompare());
            int width = sizeList.get(0).getWidth();
            int height = sizeList.get(0).getHeight();
            Log.e("Width, Height", Integer.toString(width) + ", " + Integer.toString(height));
            ImageReader reader = ImageReader.newInstance(width,height, nImageFormat, 1);
            reader.setOnImageAvailableListener(this, null);
            nSurface = reader.getSurface();
        }

        @Override
        public void onImageAvailable(@NonNull ImageReader reader) {
            reader.acquireLatestImage().close();
        }
    }
    ImageReaderListener mImageReaderListener = new ImageReaderListener();

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
        try {
            mImageReaderListener.setup(cameraManager.getCameraCharacteristics(cameraIds[0]));
            cameraManager.openCamera(cameraIds[0], mCameraState, null);
        }
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
        //surfaceList.add(mSurfaceListener.nSurface);
        surfaceList.add(mImageReaderListener.nSurface);
        for (Surface surface : surfaceList) {
            mCameraState.nBuilder.addTarget(surface);
        }
        mCameraState.nBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(30,30));
        Log.e("F", "3A");
        try { mCameraState.nCamera.createCaptureSession(surfaceList, mSessionState, null); }
        catch (CameraAccessException e) { }
    }

    //----------------------------------------------------------------------------------------------

    class SessionCapture extends CameraCaptureSession.CaptureCallback {
        long firstTimestamp = 0;
        long lastTimestamp;

        long frameLimit = 100;
        long frameCount = 0;

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session,
                                     @NonNull CaptureRequest request,
                                     long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            if (frameNumber > frameLimit) {
                try {
                    session.stopRepeating();
                }
                catch (CameraAccessException e) { }
            }
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            frameCount += 1;
            if (firstTimestamp == 0) {
                firstTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP);
            }
            lastTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP);
        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session,
                                               int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
            float meanElapsed = (lastTimestamp - firstTimestamp) / (float) frameCount;
            float fps = 1e9f / meanElapsed;
            Log.e(Thread.currentThread().getName(), "Average FPS: " + Float.toString(fps));
            session.close();
            finishAffinity();
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
}
