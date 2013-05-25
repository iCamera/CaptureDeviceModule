/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

import java.util.List;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.proxy.TiViewProxy;

import android.graphics.Color;
import android.hardware.Camera.Size;

import android.content.Context;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;

public class CameraLayout extends FrameLayout {
	private static final String TAG = "CameraLayout";

	private SurfaceView preview;
	private PreviewLayout previewLayout;

	public static List<Size> supportedPreviewSizes;
	public static Size optimalPreviewSize;

	public static TiViewProxy overlayProxy = null;

	private static class PreviewLayout extends FrameLayout
	{
		private double aspectRatio = 1;

		public PreviewLayout(Context context)
		{
			super(context);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
		{
			int previewWidth = MeasureSpec.getSize(widthMeasureSpec);
			int previewHeight = MeasureSpec.getSize(heightMeasureSpec);

			// Set the preview size to the most optimal given the target size
			optimalPreviewSize = getOptimalPreviewSize(supportedPreviewSizes, previewWidth, previewHeight);
			if (optimalPreviewSize != null) {
				if (previewWidth > previewHeight) {
					aspectRatio = (double) optimalPreviewSize.width / optimalPreviewSize.height;
				} else {
					aspectRatio = (double) optimalPreviewSize.height / optimalPreviewSize.width;
				}
			}

			// Resize the preview frame with correct aspect ratio.
			if (previewWidth > previewHeight * aspectRatio) {
				previewWidth = (int) (previewHeight * aspectRatio + .5);

			} else {
				previewHeight = (int) (previewWidth / aspectRatio + .5);
			}

			super.onMeasure(MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY));
		}
	}


	public CameraLayout(Context context)
	{
		super(context);

		// create camera preview
		preview = new SurfaceView(context);
		SurfaceHolder previewHolder = preview.getHolder();
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		previewLayout = new PreviewLayout(context);
		setBackgroundColor(Color.BLACK);
		addView(previewLayout, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
			LayoutParams.MATCH_PARENT, Gravity.CENTER));
	}

	public void addSurfaceCallback(SurfaceHolder.Callback callback) {
		preview.getHolder().addCallback(callback);
	}

	/**
	 * Computes the optimal preview size given the target display size and aspect ratio.
	 * 
	 * @param supportPreviewSizes
	 *            a list of preview sizes the camera supports
	 * @param targetSize
	 *            the target display size that will render the preview
	 * @return the optimal size of the preview
	 */
	private static Size getOptimalPreviewSize(List<Size> sizes, int w, int h)
	{
		final double ASPECT_TOLERANCE = 0.01;
		double targetRatio = (double) w / h;
		if (sizes == null) {
			return null;
		}
		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
				continue;
			}
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			Log.w(TAG, "No preview size found that matches the aspect ratio.", Log.DEBUG_MODE);
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	@Override
	protected void onAttachedToWindow () {
		previewLayout.addView(preview, new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		/*
		this.addView(localOverlayProxy.getOrCreateView().getNativeView(), new FrameLayout.LayoutParams(
			LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		*/
	}

	@Override
	protected void onDetachedFromWindow()
	{
		previewLayout.removeView(preview);
		//cameraLayout.removeView(localOverlayProxy.getOrCreateView().getNativeView());
	}

}