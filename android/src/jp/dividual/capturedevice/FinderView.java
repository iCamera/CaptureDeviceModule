/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollObject;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.media.MediaModule;

import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

import android.content.Context;
import android.graphics.Rect;
import android.view.Surface;
import android.view.SurfaceHolder;


public class FinderView extends TiUIView implements SurfaceHolder.Callback, Camera.PictureCallback, Camera.ShutterCallback, Camera.AutoFocusCallback {

	private static final String TAG = "FinderView";
	private static Camera camera;

	private CameraLayout cameraLayout;
	private boolean previewRunning = false;
	private int currentRotation;
	private int currentRotationDegrees = 0;
	private int currentFacing = Camera.CameraInfo.CAMERA_FACING_BACK;

	public static FinderView finderView = null;

	public static KrollObject callbackContext;
	public static KrollFunction successCallback, errorCallback, cancelCallback;
	public static boolean saveToPhotoGallery = false;

	public FinderView(TiViewProxy proxy) {
		super(proxy);		

		Context context = proxy.getActivity();

		// set overall layout - will populate in onResume
		cameraLayout = new CameraLayout(context);
		cameraLayout.addSurfaceCallback(this);
		setNativeView(cameraLayout);

		finderView = this;

		this.openCamera(currentFacing);
	}

	private void openCamera(int facing) {
		if (CaptureDeviceModule.isFroyo()) {
			this.openCameraFroyo(facing);
		} else {
			this.openCameraGingerbread(facing);
		}
		if (camera != null) {
			CameraLayout.supportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();
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

		int cameraId;
		if (facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			cameraId = CaptureDeviceModule.frontCameraId;
		} else {
			cameraId = CaptureDeviceModule.backCameraId;
		}
		camera = Camera.open(cameraId);
		currentFacing = facing;
	}

	public void setFlashMode(String value) {
		Parameters param = FinderView.camera.getParameters();
		List<String> supportedFlashModes = param.getSupportedFlashModes();
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

	public void switchCamera(int facing) {
		if (currentFacing == facing)
			return;
		this.releaseCamera();
		this.openCamera(facing);
		this.proxy.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					CameraLayout layout = (CameraLayout) getNativeView();
					updateCameraParameters();
					resumePreview(layout.getSurfaceHolder());
				}
			});
	}

	public void setFocusAreas(KrollDict options) {
		//Log.d("CAMERA!", options.toString());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			int maxNumFocusAreas = camera.getParameters().getMaxNumFocusAreas();
			int maxNumMeteringAreas = camera.getParameters().getMaxNumMeteringAreas();

			/*
			// output camera params
			String focusMode = camera.getParameters().getFocusMode();
			Log.d("CAMERA!", String.format("focusMode:%s focusAreas:%d meteringAreas:%d", focusMode, maxNumFocusAreas, maxNumMeteringAreas));
			List<String> supportedFocusModes = camera.getParameters().getSupportedFocusModes();
			if(supportedFocusModes != null){
				for(String fm: supportedFocusModes){
					Log.d("CAMERA!", String.format("fm:%s", fm));
				}
			}
			*/

			Double xd = TiConvert.toDouble(options.get("x"));
			Double yd = TiConvert.toDouble(options.get("y"));
			int minArea = -1000;
			int maxArea = 1000;
			int areaSize = maxArea - minArea;
			int focusSize = 10;
			int x1 = (int)((areaSize * xd) - (areaSize / 2)) - (focusSize / 2);
			int y1 = (int)((areaSize * yd) - (areaSize / 2)) - (focusSize / 2);

			//Log.d("CAMERA!", String.format("yd:%f y1:%d areaSize:%d focusSize:%d", yd, y1, areaSize, focusSize));

			int x2 = x1 + focusSize;
			int y2 = y1 + focusSize;
			int focusWeight = 1000;

			if(x1 < minArea){
				x1 = minArea;
				x2 = minArea + focusSize;
			}
			if(x2 > maxArea){
				x1 = maxArea - focusSize;
				x2 = maxArea;
			}
			if(y1 < minArea){
				y1 = minArea;
				y2 = y1 + focusSize;
			}
			if(y2 > maxArea){
				y1 = maxArea - focusSize;
				y2 = maxArea;
			}

			//Log.d("CAMERA!", String.format("xy1:%d,%d xy2:%d,%d w:%d h:%d size:%d", x1, y1, x2, y2, x2-x1, y2-y1, focusSize));

			List<Camera.Area> focusList = new ArrayList<Camera.Area>();
			focusList.add(new Camera.Area(new Rect(x1, y1, x2, y2), focusWeight));
			if(maxNumFocusAreas > 0){
				camera.getParameters().setFocusAreas(focusList);
			}
			if(maxNumMeteringAreas > 0){
				camera.getParameters().setMeteringAreas(focusList);
			}

			AutoFocusCallback focusCallback = new AutoFocusCallback(){
				public void onAutoFocus(boolean success, Camera camera){
					if(success){
						Log.d("CAMERA!", "focusCallback success!!");
					}else{
						Log.d("CAMERA!", "focusCallback faild!!");
					}
					camera.cancelAutoFocus();
					fireEvent(CaptureDeviceModule.EVENT_FOCUS_COMPLETE, new KrollDict());
				}
			};
			camera.autoFocus(focusCallback);
		}
	}

	public void takePhoto() {
		String focusMode = camera.getParameters().getFocusMode();
		if (!(focusMode.equals(Parameters.FOCUS_MODE_EDOF) || focusMode.equals(Parameters.FOCUS_MODE_FIXED) || focusMode
			.equals(Parameters.FOCUS_MODE_INFINITY))) {
			camera.autoFocus(this);
		} else {
			camera.takePicture(this, null, this);
		}
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

		if (saveToPhotoGallery) {
			//saveToPhotoGallery(data);
		}

		TiBlob imageData = TiBlob.blobFromData(data);
		KrollDict dict = CaptureDeviceModule.createDictForImage(imageData, currentRotationDegrees);
		fireEvent(CaptureDeviceModule.EVENT_IMAGE_PROCESSED, dict);

		//cancelCallback = null;
		//cameraActivity.finish();
	}

	public void surfaceChanged(SurfaceHolder previewHolder, int format, int width, int height)
	{
		if (camera == null) {
			return;
		}

		int rotation = this.proxy.getActivity().getWindowManager().getDefaultDisplay().getRotation();
		if (currentRotation == rotation && previewRunning) {
			return;
		}
		this.pausePreview(previewHolder);
		currentRotation = rotation;
		this.updateCameraParameters();
		this.resumePreview(previewHolder);
	}

	private void updateCameraParameters() {
		Parameters param = camera.getParameters();
		int orientation = TiApplication.getInstance().getResources().getConfiguration().orientation;
		// The camera preview is always displayed in landscape mode. Need to rotate the preview according to
		// the current orientation of the device.
		int degrees = 0;
		switch (currentRotation) {
		case Surface.ROTATION_0:
			if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				// The "natural" orientation of the device is a portrait orientation, eg. phones.
				// Need to rotate 90 degrees.
				degrees = 90;
			} else {
				// The "natural" orientation of the device is a landscape orientation, eg. tablets.
				// Set the camera to the starting position (0 degree).
				degrees = 0;
			}
			break;
		case Surface.ROTATION_90:
			if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				degrees = 0;
			} else {
				degrees = 270;
			}
			break;
		case Surface.ROTATION_180:
			if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				degrees = 270;
			} else {
				degrees = 180;
			}
			break;
		case Surface.ROTATION_270:
			if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				degrees = 180;
			} else {
				degrees = 90;
			}
			break;
		}
		currentRotationDegrees = degrees;
		camera.setDisplayOrientation(currentRotationDegrees);

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
			camera.setParameters(param);
		}
	}

	public void surfaceCreated(SurfaceHolder previewHolder)
	{
		try {
			camera.setPreviewDisplay(previewHolder);
		} catch (Exception e) {
			onError(MediaModule.UNKNOWN_ERROR, "Unable to setup preview surface: " + e.getMessage());
			cancelCallback = null;
			//finish();
			return;
		}
		currentRotation = this.proxy.getActivity().getWindowManager().getDefaultDisplay().getRotation();
	}

	// make sure to call release() otherwise you will have to force kill the app before
	// the built in camera will open
	public void surfaceDestroyed(SurfaceHolder previewHolder)
	{
		this.releaseCamera();
	}

	@Override
	public void release()
	{
		super.release();
		finderView = null;
		this.releaseCamera();
	}

	private static void onError(int code, String message)
	{
		if (errorCallback == null) {
			Log.e(TAG, message);
			return;
		}

		KrollDict dict = new KrollDict();
		dict.putCodeAndMessage(code, message);
		dict.put(TiC.PROPERTY_MESSAGE, message);

		errorCallback.callAsync(callbackContext, dict);
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
