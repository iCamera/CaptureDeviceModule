/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiContext;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;

import ti.modules.titanium.media.MediaModule;

@Kroll.module(name="CaptureDeviceModule", id="jp.dividual.capturedevice")
public class CaptureDeviceModule extends KrollModule
{
	public static final float PHOTO_WIDTH_CONTENT = 640.0f;
	public static final float PHOTO_WIDTH_THUMBNAIL = 150.0f;

	public static final String EVENT_SHUTTER = "shutter";
	public static final String EVENT_IMAGE_PROCESSED = "imageProcessed";
	public static final String EVENT_FOCUS_COMPLETE = "focusComplete";

	public static final String EVENT_PROPERTY_ORIGINAL = "original";
	public static final String EVENT_PROPERTY_CONTENT = "content";
	public static final String EVENT_PROPERTY_THUMBNAIL = "thumbnail";

	public static final String CAMERA_DEVICE_BACK = "back";
	public static final String CAMERA_DEVICE_FRONT = "front";

	public static final String CAMERA_PROPERTY_CAMERA_ID_SAMSUNG = "camera-id";

	protected static final String FEATURE_CAMERA_FRONT = "android.hardware.camera.front"; // Needed until api 9 is our minimum supported.

	public static int frontCameraId = -1;
	public static int backCameraId = -1;
	public static Boolean hasFroyoFrontCamera = null;

	public CaptureDeviceModule(TiContext tiContext) {
		super(tiContext);
		// Multiple cameras are supported only above froyo
		if (isGingerbread()) {
			this.scanCameraIds();
		}
	}

	public static boolean isFroyo() {
		return Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO;
	}

	public static boolean isGingerbread() {
		return !isFroyo();
	}

	public static boolean isFrontCameraSupported() {
		if (CaptureDeviceModule.isFroyo()) {
			if (CaptureDeviceModule.hasFroyoFrontCamera == null) {
				Camera camera = Camera.open();
				Camera.Parameters params = camera.getParameters();
				params.set(CaptureDeviceModule.CAMERA_PROPERTY_CAMERA_ID_SAMSUNG, 2);
				try {
					camera.setParameters(params);
					CaptureDeviceModule.hasFroyoFrontCamera = true;
				} catch (RuntimeException e) {
					CaptureDeviceModule.hasFroyoFrontCamera = false;
				}
			}
			return CaptureDeviceModule.hasFroyoFrontCamera;
		} else {
			TiApplication appContext = TiApplication.getInstance();
			Activity activity = appContext.getCurrentActivity();
			PackageManager pm = activity.getPackageManager();
			return pm.hasSystemFeature(CaptureDeviceModule.FEATURE_CAMERA_FRONT);
		}
	}

	public static boolean isBackCameraSupported() {
		TiApplication appContext = TiApplication.getInstance();
		Activity activity = appContext.getCurrentActivity();
		PackageManager pm = activity.getPackageManager();
		return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
	}

	public static KrollDict createDictForImage(TiBlob imageData, String mimeType) {
		KrollDict d = new KrollDict();
		d.putCodeAndMessage(MediaModule.NO_ERROR, null);
		d.put(EVENT_PROPERTY_ORIGINAL, imageData);

		int width = -1;
		int height = -1;

		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;

		// We only need the ContentResolver so it doesn't matter if the root or current activity is used for
		// accessing it
		BitmapFactory.decodeStream(imageData.getInputStream(), null, opts);

		width = opts.outWidth;
		height = opts.outHeight;

		float contentHeight = height * PHOTO_WIDTH_CONTENT / width;
		float thumbnailHeight = height * PHOTO_WIDTH_THUMBNAIL / width;

		d.put(EVENT_PROPERTY_CONTENT, imageData.imageAsResized(PHOTO_WIDTH_CONTENT, contentHeight));
		d.put(EVENT_PROPERTY_THUMBNAIL, imageData.imageAsResized(PHOTO_WIDTH_THUMBNAIL, thumbnailHeight));

		return d;
	}

	private void scanCameraIds() {
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		for (int cameraIndex = 0; cameraIndex < Camera.getNumberOfCameras(); cameraIndex++) {
			Camera.getCameraInfo(cameraIndex, cameraInfo);
			if (frontCameraId == -1 && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				frontCameraId = cameraIndex;
			}
			if (backCameraId == -1 && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				backCameraId = cameraIndex;
			}
		}		
	}
}
