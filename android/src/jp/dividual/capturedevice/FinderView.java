/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollObject;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiLifecycle;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.media.MediaModule;

import android.app.Activity;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.os.Build;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;


public class FinderView extends TiUIView implements SurfaceHolder.Callback, Camera.PictureCallback, Camera.ShutterCallback, Camera.AutoFocusCallback, TiLifecycle.OnLifecycleEvent {

	private static final String TAG = "FinderView";
	private static Camera camera;

	private CameraLayout cameraLayout;
	private boolean previewRunning = false;
	private int currentUIRotation;
	private int currentUIRotationDegrees = 0;
	private int currentFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
	private int currentCameraId = 0;
	private List<String> supportedFlashModes = null;
	private boolean focusAreaSupported = false;
	private boolean meteringAreaSupported = false;
	private List<Area> focusAreas = null;
	private List<Area> meteringAreas = null;
	private Matrix focusMatrix;
	private int previewWidth;
	private int previewHeight;

	public static FinderView finderView = null;

	public static KrollObject callbackContext;
	public static boolean saveToPhotoGallery = false;

	public FinderView(TiViewProxy proxy, boolean finderStart) {
		super(proxy);		

		focusMatrix = new Matrix();

		Context context = proxy.getActivity();
		// set overall layout - will populate in onResume
		cameraLayout = new CameraLayout(context);
		cameraLayout.addSurfaceCallback(this);
		setNativeView(cameraLayout);

		finderView = this;

		if(finderStart){
			this.openCamera(currentFacing);
		}

		if (context instanceof TiBaseActivity) {
			((TiBaseActivity)context).addOnLifecycleEventListener(this);
		}
	}

	public void start() {
		this.openCamera(currentFacing);
	}

	public void stop() {
		this.releaseCamera();
	}

	private void openCamera(int facing) {
		if (CaptureDeviceModule.isFroyo()) {
			this.openCameraFroyo(facing);
		} else {
			this.openCameraGingerbread(facing);
		}
		if (camera != null) {
			this.initializeAfterCameraOpen();
		} else {
			onError(MediaModule.UNKNOWN_ERROR, "Unable to access the first back-facing camera.");
			//finish();
		}
	}

	private void openCameraFroyo(int facing) {
		camera = Camera.open();
		if (facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			// Samsung Galaxy S/Tab
			Parameters param = camera.getParameters();
			param.set(CaptureDeviceModule.CAMERA_PROPERTY_CAMERA_ID_SAMSUNG, 2);
			try {
				camera.setParameters(param);
				currentFacing = facing;
			} catch (RuntimeException e) {
				// If we can't set front camera it means that device
				// hasn't got "camera-id". Maybe it's not a Galaxy S.
				currentFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
			}
		} else {
			currentFacing = facing;
		}
	}

	private void openCameraGingerbread(int facing) {
		boolean hasFront = CaptureDeviceModule.isFrontCameraSupported();
		boolean hasBack = CaptureDeviceModule.isBackCameraSupported();
		if (!hasFront && facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			if (hasBack) {
				facing = Camera.CameraInfo.CAMERA_FACING_BACK;
			} else {
				// TODO: notify error
				return;
			}
		}
		if (!hasBack && facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
			if (hasFront) {
				facing = Camera.CameraInfo.CAMERA_FACING_FRONT;
			} else {
				// TODO: notify error
				return;
			}
		}

		if (facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			currentCameraId = CaptureDeviceModule.frontCameraId;
		} else {
			currentCameraId = CaptureDeviceModule.backCameraId;
		}
		camera = Camera.open(currentCameraId);
		currentFacing = facing;
	}

	private void initializeAfterCameraOpen() {
		this.broadcastCapabilities();
		this.setMatrix();
	}

	private void broadcastCapabilities() {
		if (camera == null) {
			return;
		}

		Parameters param = FinderView.camera.getParameters();
		CameraLayout.supportedPreviewSizes = param.getSupportedPreviewSizes();
		CameraLayout.optimalPreviewSize = null;
		this.getNativeView().requestLayout();
		this.supportedFlashModes = param.getSupportedFlashModes();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			this.focusAreaSupported = (param.getMaxNumFocusAreas() > 0
									   && isSupported(Parameters.FOCUS_MODE_AUTO,
													  param.getSupportedFocusModes()));
			this.meteringAreaSupported = (param.getMaxNumMeteringAreas() > 0);
		}
		KrollDict dict = new KrollDict();
		dict.put(CaptureDeviceModule.EVENT_PROPERTY_MANUAL_FOCUS, this.focusAreaSupported);
		dict.put(CaptureDeviceModule.EVENT_PROPERTY_MANUAL_METERING, this.meteringAreaSupported);
		if (this.supportedFlashModes != null) {
			dict.put(CaptureDeviceModule.EVENT_PROPERTY_FLASH_MODES, this.supportedFlashModes.toArray(new String[this.supportedFlashModes.size()]));
		} else {
			dict.put(CaptureDeviceModule.EVENT_PROPERTY_FLASH_MODES, new String[0]);
		}
		fireEvent(CaptureDeviceModule.EVENT_CAMERA_OPEN, dict);
	}

    private boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

	public void setFlashMode(String value) {
		Parameters param = FinderView.camera.getParameters();
		if (supportedFlashModes == null) {
			Log.d(TAG, "Camera has no flash", Log.DEBUG_MODE);
			return;
		}
		if (!supportedFlashModes.contains(value)) {
			if (value == Parameters.FLASH_MODE_ON && supportedFlashModes.contains(Parameters.FLASH_MODE_TORCH)) {
				value = Parameters.FLASH_MODE_TORCH;
			} else {
				Log.d(TAG, "Camera flash mode is not supported: " + value + "(" + Arrays.toString(supportedFlashModes.toArray()) + ")", Log.DEBUG_MODE);
				return;
			}
		}
		param.setFlashMode(value);
		FinderView.camera.setParameters(param);
		Log.d(TAG, "Set camera flash mode to: " + value, Log.DEBUG_MODE);
	}

	public void switchCamera(final int facing) {
		if (currentFacing == facing)
			return;
		this.releaseCamera();
		this.proxy.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					openCamera(facing);
					CameraLayout layout = (CameraLayout) getNativeView();
					updateCameraParameters();
					resumePreview(layout.getSurfaceHolder());
				}
			});
	}

	public void focusAndExposureAtPoint(KrollDict options) {
		//Log.d("CAMERA!", options.toString());
		if (this.focusAreaSupported || this.meteringAreaSupported) {
			Parameters param = camera.getParameters();
			String focusMode = param.getFocusMode();
			Log.d(TAG, String.format("focusMode:%s", focusMode), Log.DEBUG_MODE);
			/*
			// output camera params

			List<String> supportedFocusModes = camera.getParameters().getSupportedFocusModes();
			if(supportedFocusModes != null){
				for(String fm: supportedFocusModes){
					Log.d("CAMERA!", String.format("fm:%s", fm));
				}
			}
			*/

			Double xd = TiConvert.toDouble(options.get("x"));
			Double yd = TiConvert.toDouble(options.get("y"));

			View touchableView = this.getNativeView();
			int width = touchableView.getWidth();
			int height = touchableView.getHeight();
			if (previewWidth != width || previewHeight != height) {
				this.setMatrix();
			}

			int focusSize = 40;
			int x = (int)(width * xd);
			int y = (int)(height * yd);

			Log.d(TAG, String.format("xd:%f x:%d width:%d", xd, x, width), Log.DEBUG_MODE);
			Log.d(TAG, String.format("yd:%f y:%d height:%d", yd, y, height), Log.DEBUG_MODE);

			if(this.focusAreaSupported){
				initializeFocusAreas(focusSize, focusSize, x, y, width, height);
				param.setFocusAreas(focusAreas);
			}
			if(this.meteringAreaSupported){
				initializeFocusAreas(focusSize, focusSize, x, y, width, height);
				param.setMeteringAreas(meteringAreas);
			}

			camera.setParameters(param);
			Camera.AutoFocusCallback focusCallback = new Camera.AutoFocusCallback(){
				public void onAutoFocus(boolean success, Camera camera){
					if(success){
						Log.d(TAG, "focusCallback success!!", Log.DEBUG_MODE);
					}else{
						Log.d(TAG, "focusCallback faild!!", Log.DEBUG_MODE);
					}
					camera.cancelAutoFocus();
					fireEvent(CaptureDeviceModule.EVENT_FOCUS_COMPLETE, new KrollDict());

					if(focusAreaSupported){
						Log.d(TAG, "RAW GET focus area: " + camera.getParameters().get("focus-areas"), Log.DEBUG_MODE);
					}
				}
			};
			camera.autoFocus(focusCallback);
		}
	}

	private void initializeFocusAreas(int focusWidth, int focusHeight,
							   int x, int y, int previewWidth, int previewHeight) {
        if (focusAreas == null) {
            focusAreas = new ArrayList<Area>();
            focusAreas.add(new Area(new Rect(), 1000));
        }
        calculateTapArea(focusWidth, focusHeight, 1.5f, x, y, previewWidth, previewHeight,
                ((Area) focusAreas.get(0)).rect);
	}

	private void initializeMeteringAreas(int focusWidth, int focusHeight,
							   int x, int y, int previewWidth, int previewHeight) {
        if (meteringAreas == null) {
            meteringAreas = new ArrayList<Area>();
            meteringAreas.add(new Area(new Rect(), 1000));
        }
        calculateTapArea(focusWidth, focusHeight, 1.5f, x, y, previewWidth, previewHeight,
                ((Area) meteringAreas.get(0)).rect);
	}

    private void calculateTapArea(int focusWidth, int focusHeight, float areaMultiple,
            int x, int y, int previewWidth, int previewHeight, Rect rect) {
        int areaWidth = (int) (focusWidth * areaMultiple);
        int areaHeight = (int) (focusHeight * areaMultiple);
        int left = Util.clamp(x - areaWidth / 2, 0, previewWidth - areaWidth);
        int top = Util.clamp(y - areaHeight / 2, 0, previewHeight - areaHeight);

        RectF rectF = new RectF(left, top, left + areaWidth, top + areaHeight);
		Log.d(TAG, "calculateTapArea before rectF: " + rectF.toString(), Log.DEBUG_MODE);
        focusMatrix.mapRect(rectF);
		Log.d(TAG, "calculateTapArea mapped rectF: " + rectF.toString(), Log.DEBUG_MODE);
        Util.rectFToRect(rectF, rect);
    }

	/* Call when facing, displayOrientation has been changed */
    private void setMatrix() {
        if (true) {
            Matrix tmpMatrix = new Matrix();
			View touchableView = this.getNativeView();
			previewWidth = touchableView.getWidth();
			previewHeight = touchableView.getHeight();
			Boolean mirror = isFacingFront();
			//Log.d(TAG, String.format("setMatrix mirror:%b width:%d height:%d", mirror, previewWidth, previewHeight), Log.DEBUG_MODE);
			Util.prepareMatrix(tmpMatrix, mirror, currentUIRotationDegrees,
                    previewWidth, previewHeight);
            // In face detection, the matrix converts the driver coordinates to UI
            // coordinates. In tap focus, the inverted matrix converts the UI
            // coordinates to driver coordinates.
            tmpMatrix.invert(focusMatrix);
        }
    }

	public void takePhoto(KrollDict options) {
		saveToPhotoGallery = false;
		if(options.containsKey("saveToDevice")){
			saveToPhotoGallery = TiConvert.toBoolean(options.get("saveToDevice"));
		}
		Parameters param = camera.getParameters();
		if(options.containsKey("lat") && options.containsKey("lng")){
			param.removeGpsData();
			param.setGpsTimestamp(System.currentTimeMillis() / 1000);
			param.setGpsAltitude(0);

			Double lat = TiConvert.toDouble(options.get("lat"));
			param.setGpsLatitude(lat);
			//Log.d(TAG, String.format("lat: %f", lat));

			Double lng = TiConvert.toDouble(options.get("lng"));
			param.setGpsLongitude(lng);
			//Log.d(TAG, String.format("log: %f", lng));

			camera.setParameters(param);
		}
		boolean focus = false;
		if(options.containsKey("focus")){
			focus = TiConvert.toBoolean(options.get("focus"));
		}

		String focusMode = param.getFocusMode();
		if (focus && (!(focusMode.equals(Parameters.FOCUS_MODE_EDOF) || focusMode.equals(Parameters.FOCUS_MODE_FIXED) || focusMode
			.equals(Parameters.FOCUS_MODE_INFINITY)))) {
			camera.autoFocus(this);
		} else {
			camera.takePicture(this, null, this);
		}
	}

	public void onResume(Activity activity) {
	}
	
	public void onPause(Activity activity) {
	}

	public void onDestroy(Activity activity) {
	}

	public void onStart(Activity activity) {
	}

	public void onStop(Activity activity) {	
	}

	public void onAutoFocus(boolean success, Camera camera) {
		// Take the picture when the camera auto focus completes.
		camera.takePicture(this, null, this);
	}

	public void onShutter() {
		fireEvent(CaptureDeviceModule.EVENT_SHUTTER, new KrollDict());
	}

	public void onPictureTaken(byte[] data, Camera camera) {
		camera.startPreview();

		//Log.d("LATLON", String.format("saveToPhotoGallery: %b", saveToPhotoGallery));
		if (saveToPhotoGallery) {
			CaptureDeviceModule.saveToPhotoGallery(data);
		}

		TiBlob imageData = TiBlob.blobFromData(data);
		int rotateDegrees = getCameraDisplayOrientation(true);
		if (isFacingFront()) {
			rotateDegrees = (rotateDegrees + 180) % 360;
		}
		KrollDict dict = CaptureDeviceModule.createDictForImage(imageData, rotateDegrees);
		fireEvent(CaptureDeviceModule.EVENT_IMAGE_PROCESSED, dict);
	}

	public void surfaceCreated(SurfaceHolder previewHolder)
	{
		try {
			camera.setPreviewDisplay(previewHolder);
		} catch (Exception e) {
			onError(MediaModule.UNKNOWN_ERROR, "Unable to setup preview surface: " + e.getMessage());
			//finish();
			return;
		}
		currentUIRotation = this.proxy.getActivity().getWindowManager().getDefaultDisplay().getRotation();
	}

	// make sure to call release() otherwise you will have to force kill the app before
	// the built in camera will open
	public void surfaceDestroyed(SurfaceHolder previewHolder)
	{
		this.releaseCamera();
	}

	public void surfaceChanged(SurfaceHolder previewHolder, int format, int width, int height)
	{
		if (camera == null) {
			return;
		}

		int rotation = this.proxy.getActivity().getWindowManager().getDefaultDisplay().getRotation();
		if (currentUIRotation == rotation && previewRunning) {
			return;
		}
		this.pausePreview(previewHolder);
		currentUIRotation = rotation;
		this.updateCameraParameters();
		this.resumePreview(previewHolder);
	}

	private void updateCameraParameters() {
		Parameters param = camera.getParameters();
		currentUIRotationDegrees = this.getCameraDisplayOrientation();
		camera.setDisplayOrientation(currentUIRotationDegrees);
		this.setMatrix();
		Log.d(TAG, "Set currentUIRotationDegrees to: " + ((Number)currentUIRotationDegrees).toString(), Log.DEBUG_MODE);

		// Set appropriate focus mode if supported.
		List<String> supportedFocusModes = param.getSupportedFocusModes();
		if (supportedFocusModes.contains(MediaModule.FOCUS_MODE_CONTINUOUS_PICTURE)) {
			param.setFocusMode(MediaModule.FOCUS_MODE_CONTINUOUS_PICTURE);
		} else if (supportedFocusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
			param.setFocusMode(Parameters.FOCUS_MODE_AUTO);
		} else if (supportedFocusModes.contains(Parameters.FOCUS_MODE_MACRO)) {
			param.setFocusMode(Parameters.FOCUS_MODE_MACRO);
		}

		if (CameraLayout.optimalPreviewSize != null) {
			param.setPreviewSize(CameraLayout.optimalPreviewSize.width, CameraLayout.optimalPreviewSize.height);
		}
		camera.setParameters(param);
	}

	private int getCameraDisplayOrientation() {
		return getCameraDisplayOrientation(false);
	}

	private int getCameraDisplayOrientation(boolean forceUpdate) {
		if (forceUpdate) {
			currentUIRotation = this.proxy.getActivity().getWindowManager().getDefaultDisplay().getRotation();
		}
		int degrees = 0;
		switch (currentUIRotation) {
		case Surface.ROTATION_0: degrees = 0; break;
		case Surface.ROTATION_90: degrees = 90; break;
		case Surface.ROTATION_180: degrees = 180; break;
		case Surface.ROTATION_270: degrees = 270; break;
		}

		int result;
		int cameraOrientation = this.getCameraOrientation();
		if (this.isFacingFront()) {
			result = (cameraOrientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (cameraOrientation - degrees + 360) % 360;
		}
		return result;
	}

	private int getCameraOrientation() {
		int orientation = 0;
		if (CaptureDeviceModule.isFroyo()) {
			if (this.isFacingFront()) {
				orientation = 270;
			} else {
				orientation = 90;
			}
		} else {
			Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(currentCameraId, info);
			orientation = info.orientation;
		}
		return orientation;
	}

	private boolean isFacingFront() {
		return currentFacing == Camera.CameraInfo.CAMERA_FACING_FRONT;
	}

	@Override
	public void release()
	{
		Log.d(TAG, "Releasing everything", Log.DEBUG_MODE);
		super.release();
		finderView = null;
		this.releaseCamera();
	}

	private void onError(int code, String message)
	{
		KrollDict dict = new KrollDict();
		dict.putCodeAndMessage(code, message);
		dict.put(TiC.PROPERTY_MESSAGE, message);
		fireEvent(CaptureDeviceModule.EVENT_ERROR, dict);
	}

	private void pausePreview(SurfaceHolder previewHolder) {
		if (previewRunning) {
			try {
				camera.stopPreview();
			} catch (Exception e) {
				// ignore: tried to stop a non=existent preview
			}
		}
	}

	private void resumePreview(SurfaceHolder previewHolder) {
		try {
			camera.setPreviewDisplay(previewHolder);
			previewRunning = true;
			camera.startPreview();
		} catch (Exception e) {
			onError(MediaModule.UNKNOWN_ERROR, "Unable to setup preview surface: " + e.getMessage());
			//finish();
			return;
		}
	}

	private void releaseCamera() {
		stopPreview();
		if (camera != null) {
			try {
				camera.release();
				camera = null;
			} catch (Throwable t) {
				Log.d(TAG, "Camera is not open, unable to release", Log.DEBUG_MODE);
			}
		}
	}

	private void stopPreview()
	{
		if (camera == null || !previewRunning) {
			return;
		}

		camera.stopPreview();
		previewRunning = false;
	}
}
