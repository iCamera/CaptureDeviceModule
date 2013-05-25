/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

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

	private TiViewProxy localOverlayProxy = null;
	private CameraLayout cameraLayout;
	private boolean previewRunning = false;
	private int currentRotation;
	private PictureCallback jpegCallback;

	public static TiViewProxy overlayProxy = null;
	public static FinderView finderView = null;

	public static KrollObject callbackContext;
	public static KrollFunction successCallback, errorCallback, cancelCallback;
	public static boolean saveToPhotoGallery = false;

	public FinderView(TiViewProxy proxy) {
		super(proxy);		

		Context context = proxy.getActivity();

		// set preview overlay
		localOverlayProxy = overlayProxy;
		overlayProxy = null; // clear the static object once we have a local reference

		// set overall layout - will populate in onResume
		cameraLayout = new CameraLayout(context);
		cameraLayout.addSurfaceCallback(this);
		setNativeView(cameraLayout);

		finderView = this;

		camera = Camera.open();
		if (camera != null) {
			CameraLayout.supportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();
		} else {
			onError(MediaModule.UNKNOWN_ERROR, "Unable to access the first back-facing camera.");
			//finish();
		}

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

	public void takePhoto()
	{
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
		if (previewRunning) {
			try {
				camera.stopPreview();
			} catch (Exception e) {
				// ignore: tried to stop a non=existent preview
			}
		}

		currentRotation = rotation;
		Parameters param = camera.getParameters();
		int orientation = TiApplication.getInstance().getResources().getConfiguration().orientation;
		// The camera preview is always displayed in landscape mode. Need to rotate the preview according to
		// the current orientation of the device.
		switch (rotation) {
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
		stopPreview();
		if (camera != null) {
			camera.release();
			camera = null;
		}
	}

	@Override
	public void release()
	{
		super.release();
		stopPreview();

		finderView = null;

		try {
			camera.release();
			camera = null;
		} catch (Throwable t) {
			Log.d(TAG, "Camera is not open, unable to release", Log.DEBUG_MODE);
		}
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

	private void stopPreview()
	{
		if (camera == null || !previewRunning) {
			return;
		}

		camera.stopPreview();
		previewRunning = false;
	}
}
