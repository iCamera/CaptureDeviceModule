/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;

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
		return true;
	}

	@Kroll.method
	public void setFlashModeAuto() {
	}

	@Kroll.method
	public void setFlashModeOff() {
	}

	@Kroll.method
	public void setFlashModeOn() {
	}

	@Kroll.method
	public void changeToFrontCamera() {
	}

	@Kroll.method
	public void changeToBackCamera() {
	}
}
