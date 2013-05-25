/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;

@Kroll.proxy(creatableInModule=CaptureDeviceModule.class)
public class FinderProxy extends TiViewProxy
{
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
	
}
