package jp.dividual.capturedevice;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;

@Kroll.proxy(creatableInModule=ColoredViewModule.class)
public class ColoredViewProxy extends TiViewProxy
{
	private ColoredView coloredView;

	/*	
	public ColoredViewProxy(TiContext tiContext) {
		super.TiViewProxy(tiContext);
	}
	*/
	
	@Override
	public TiUIView createView(Activity activity) {
		coloredView = new ColoredView(this);
		return coloredView;
	}
	
}
