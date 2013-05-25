package jp.dividual.capturedevice;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;

@Kroll.proxy(creatableInModule=ColoredViewModule.class)
public class FinderProxy extends TiViewProxy
{
	private FinderView finderView;

	/*	
	public ColoredViewProxy(TiContext tiContext) {
		super.TiViewProxy(tiContext);
	}
	*/
	
	@Override
	public TiUIView createView(Activity activity) {
		finderView = new FinderView(this);
		return finderView;
	}
	
}
