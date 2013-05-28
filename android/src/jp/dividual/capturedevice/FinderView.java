/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

import java.util.Arrays;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollObject;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.media.MediaModule;

import android.os.Build;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;


public class FinderView extends TiUIView implements SurfaceHolder.Callback {

	private static final String TAG = "FinderView";
	private static Camera camera;

	private CameraLayout cameraLayout;
	private boolean previewRunning = false;
	private int currentRotation;
	private int currentFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
	private PictureCallback jpegCallback;

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

		jpegCallback = new PictureCallback() {
				public void onPictureTaken(byte[] data, Camera camera) {
					camera.startPreview();

					if (saveToPhotoGallery) {
						//saveToPhotoGallery(data);
					}

					TiBlob imageData = TiBlob.blobFromData(data);
					KrollDict dict = CaptureDeviceModule.createDictForImage(imageData, "image/jpeg");
					fireEvent(CaptureDeviceModule.EVENT_IMAGE_PROCESSED, dict);

					//cancelCallback = null;
					//cameraActivity.finish();
				}
			};
	}

	private void openCamera(int facing) {
		boolean hasFront = this.isFrontCameraSupported();
		boolean hasBack = this.isBackCameraSupported();
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
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
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
			param.set("camera-id", 2);
			try {
				camera.setParameters(param);
				currentFacing = facing;
			} catch (RuntimeException e) {
				// If we can't set front camera it means that device hasn't got "camera-id". Maybe it's not Galaxy S.
				currentFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
			}
		} else {
			currentFacing = facing;
		}
	}

	private void openCameraGingerbread(int facing) {
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
		if (!this.isFrontCameraSupported() || !this.isBackCameraSupported()) {
			// can't switch
			return;
		}
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

	public boolean isFrontCameraSupported() {
		PackageManager pm = this.proxy.getActivity().getPackageManager();
		return pm.hasSystemFeature(CaptureDeviceModule.FEATURE_CAMERA_FRONT);
	}

	public boolean isBackCameraSupported() {
		PackageManager pm = this.proxy.getActivity().getPackageManager();
		return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
	}

	public void takePhoto() {
		String focusMode = camera.getParameters().getFocusMode();
		if (!(focusMode.equals(Parameters.FOCUS_MODE_EDOF) || focusMode.equals(Parameters.FOCUS_MODE_FIXED) || focusMode
			.equals(Parameters.FOCUS_MODE_INFINITY))) {
			AutoFocusCallback focusCallback = new AutoFocusCallback()
			{
				public void onAutoFocus(boolean success, Camera camera)
				{
					// Take the picture when the camera auto focus completes.
					camera.takePicture(null, null, jpegCallback);
				}
			};
			camera.autoFocus(focusCallback);
		} else {
			camera.takePicture(null, null, jpegCallback);
		}
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
		switch (currentRotation) {
		case Surface.ROTATION_0:
			if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				// The "natural" orientation of the device is a portrait orientation, eg. phones.
				// Need to rotate 90 degrees.
				camera.setDisplayOrientation(90);
			} else {
				// The "natural" orientation of the device is a landscape orientation, eg. tablets.
				// Set the camera to the starting position (0 degree).
				camera.setDisplayOrientation(0);
			}
			break;
		case Surface.ROTATION_90:
			if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				camera.setDisplayOrientation(0);
			} else {
				camera.setDisplayOrientation(270);
			}
			break;
		case Surface.ROTATION_180:
			if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				camera.setDisplayOrientation(270);
			} else {
				camera.setDisplayOrientation(180);
			}
			break;
		case Surface.ROTATION_270:
			if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				camera.setDisplayOrientation(180);
			} else {
				camera.setDisplayOrientation(90);
			}
			break;
		}

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
