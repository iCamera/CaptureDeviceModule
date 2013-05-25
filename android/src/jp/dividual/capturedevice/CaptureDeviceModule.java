/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiContext;

import android.graphics.BitmapFactory;

import ti.modules.titanium.media.MediaModule;

@Kroll.module(name="CaptureDeviceModule", id="jp.dividual.capturedevice")
public class CaptureDeviceModule extends KrollModule
{
	public static final float PHOTO_WIDTH_CONTENT = 640.0f;
	public static final float PHOTO_WIDTH_THUMBNAIL = 150.0f;

	public static final String EVENT_IMAGE_PROCESSED = "imageProcessed";

	public static final String EVENT_PROPERTY_ORIGINAL = "original";
	public static final String EVENT_PROPERTY_CONTENT = "content";
	public static final String EVENT_PROPERTY_THUMBNAIL = "thumbnail";

	public CaptureDeviceModule(TiContext tiContext) {
		super(tiContext);
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

}
