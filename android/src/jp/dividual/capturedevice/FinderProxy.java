/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.CameraInfo;

@Kroll.proxy(creatableInModule=CaptureDeviceModule.class)
public class FinderProxy extends TiViewProxy
{
	private static final String TAG = "FinderProxy";
	private FinderView finderView;
	private boolean finderStart = false;

	/*	
	public FinderProxy(TiContext tiContext) {
		super.TiViewProxy(tiContext);
	}
	*/
	
	@Override
	public TiUIView createView(Activity activity) {
		finderView = new FinderView(this, finderStart);
		return finderView;
	}

	@Kroll.method
	public void start() {
		if (FinderView.finderView != null) {
			FinderView.finderView.start();
		}
		finderStart = true;
	}

	@Kroll.method
	public void stop() {
		if (FinderView.finderView != null) {
			FinderView.finderView.stop();
		}
		finderStart = false;
	}

	@Kroll.method
	public void focusAndExposureAtPoint(KrollDict options) {
		FinderView.finderView.focusAndExposureAtPoint(options);
	}

	@Kroll.method
	public boolean getFocusOnTakePhoto() {
		return FinderView.finderView.getFocusOnTakePhoto();
	}

	@Kroll.method
	public void takePhoto(KrollDict options) {
		// make sure the preview / camera are open before trying to take photo
		if (FinderView.finderView != null) {
			FinderView.finderView.takePhoto(options);
		} else {
			Log.e(TAG, "Camera preview is not open, unable to take photo");
		}
	}

	@Kroll.method
	public String[] getDevices() {
		List<String> devices = new ArrayList<String>();
		if (CaptureDeviceModule.isBackCameraSupported()) {
			devices.add(CaptureDeviceModule.CAMERA_DEVICE_BACK);
		}
		if (CaptureDeviceModule.isFrontCameraSupported()) {
			devices.add(CaptureDeviceModule.CAMERA_DEVICE_FRONT);
		}
		return devices.toArray(new String[devices.size()]);
	}

	@Kroll.method
	public boolean getHasFlash() {
		return this.getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
	}

	@Kroll.method
	public void setFlashModeAuto() {
		this.setFlashMode(Parameters.FLASH_MODE_AUTO);
	}

	@Kroll.method
	public void setFlashModeOff() {
		this.setFlashMode(Parameters.FLASH_MODE_OFF);
	}

	@Kroll.method
	public void setFlashModeOn() {
		this.setFlashMode(Parameters.FLASH_MODE_ON);
	}

	private void setFlashMode(String value) {
		if (FinderView.finderView != null) {
			FinderView.finderView.setFlashMode(value);
		} else {
			Log.w(TAG, "Camera preview is not open, unable to set params");
		}
	}

	@Kroll.method
	public void changeToFrontCamera() {
		this.setCamera(CameraInfo.CAMERA_FACING_FRONT);
	}

	@Kroll.method
	public void changeToBackCamera() {
		this.setCamera(CameraInfo.CAMERA_FACING_BACK);
	}

	private void setCamera(int facing) {
		if (FinderView.finderView != null) {
			FinderView.finderView.switchCamera(facing);
		} else {
			Log.w(TAG, "Camera preview is not open, unable to set params");
		}
	}

	/**
	 * You must set the overlay view proxy before adding to a parent view.
	 */
	@Kroll.setProperty @Kroll.method
	public void setOverlay(TiViewProxy proxy) {
		CameraLayout.overlayProxy = proxy;
	}

	@Kroll.method
	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = this.getActivity().getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = this.getActivity().getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}
}
