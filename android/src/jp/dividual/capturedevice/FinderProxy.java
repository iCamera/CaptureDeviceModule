/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera.Parameters;

@Kroll.proxy(creatableInModule=CaptureDeviceModule.class)
public class FinderProxy extends TiViewProxy
{
	private static final String TAG = "FinderProxy";
	private FinderView finderView;

	/*	
	public FinderProxy(TiContext tiContext) {
		super.TiViewProxy(tiContext);
	}
	*/
	
	@Override
	public TiUIView createView(Activity activity) {
		finderView = new FinderView(this);
		return finderView;
	}

	@Kroll.method
	public void focusAndExposureAtPoint(KrollDict options) {
	}

	@Kroll.method
	public void takePhoto() {
		// make sure the preview / camera are open before trying to take photo
		if (FinderView.finderView != null) {
			FinderView.finderView.takePhoto();
		} else {
			Log.e(TAG, "Camera preview is not open, unable to take photo");
		}
	}

	@Kroll.method
	public Object[] getDevices() {
		return null;
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
	}

	@Kroll.method
	public void changeToBackCamera() {
	}

	/**
	 * You must set the overlay view proxy before adding to a parent view.
	 */
	@Kroll.setProperty @Kroll.method
	public void setOverlay(TiViewProxy proxy) {
		CameraLayout.overlayProxy = proxy;
	}
}
