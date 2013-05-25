package jp.dividual.capturedevice;

import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

public class ColoredView extends TiUIView {

	public class CostomView extends View {

		public CostomView(Context c) {
			super(c);
			
			this.setBackgroundColor(Color.GREEN);
		}

	}

	public ColoredView(TiViewProxy proxy) {
		super(proxy);		

		CostomView costomView = new CostomView(proxy.getActivity());
		
		setNativeView(costomView);
	}

}