/* -*- tab-width: 4; indent-tabs-mode: t; -*- */
package jp.dividual.capturedevice;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffTagConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.io.TiFileFactory;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import ti.modules.titanium.media.MediaModule;

@Kroll.module(name="CaptureDeviceModule", id="jp.dividual.capturedevice")
public class CaptureDeviceModule extends KrollModule
{
	private static final String TAG = "CaptureDeviceModule";
	public static final float PHOTO_WIDTH_CONTENT = 640.0f;
	public static final float PHOTO_WIDTH_THUMBNAIL = 150.0f;

	@Kroll.constant public static final String EVENT_ERROR = "error";
	@Kroll.constant public static final String EVENT_CAMERA_OPEN = "cameraOpen";
	@Kroll.constant public static final String EVENT_SHUTTER = "shutter";
	@Kroll.constant public static final String EVENT_IMAGE_PROCESSED = "imageProcessed";
	@Kroll.constant public static final String EVENT_FOCUS_COMPLETE = "focusComplete";

	@Kroll.constant public static final String EVENT_PROPERTY_ORIGINAL = "original";
	@Kroll.constant public static final String EVENT_PROPERTY_CONTENT = "content";
	@Kroll.constant public static final String EVENT_PROPERTY_THUMBNAIL = "thumbnail";
	@Kroll.constant public static final String EVENT_PROPERTY_MANUAL_FOCUS = "manualFocus";
	@Kroll.constant public static final String EVENT_PROPERTY_MANUAL_METERING = "manualMetering";
	@Kroll.constant public static final String EVENT_PROPERTY_FLASH_MODES = "flashModes";

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

	public void onResume(Activity activity) {
		//Log.d("MODULE!!!!!!!!!!", "onResume");
	}
	public void onPouse(Activity activity) {
		//Log.d("MODULE!!!!!!!!!!", "onPouse");
	}
	public void onStop(Activity activity) {
		//Log.d("MODULE!!!!!!!!!!", "onStop");
	}
	public void onDestroy(Activity activity) {
		//Log.d("MODULE!!!!!!!!!!", "onDestroy");
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
				} finally {
					camera.release();
					camera = null;
				}
			}
			return CaptureDeviceModule.hasFroyoFrontCamera;
		} else {
			Activity activity = CaptureDeviceModule.getCurrentActivity();
			PackageManager pm = activity.getPackageManager();
			return pm.hasSystemFeature(CaptureDeviceModule.FEATURE_CAMERA_FRONT);
		}
	}

	public static boolean isBackCameraSupported() {
		Activity activity = CaptureDeviceModule.getCurrentActivity();
		PackageManager pm = activity.getPackageManager();
		return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
	}

	public static void saveToPhotoGallery(byte[] data)
	{
		File imageFile = CaptureDeviceModule.createGalleryImageFile();
		try {
			FileOutputStream imageOut = new FileOutputStream(imageFile);
			imageOut.write(data);
			imageOut.close();

		} catch (FileNotFoundException e) {
			Log.e(TAG, "Failed to open gallery image file: " + e.getMessage());

		} catch (IOException e) {
			Log.e(TAG, "Failed to write image to gallery file: " + e.getMessage());
		}

		// Notify media scanner to add image to gallery.
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		Uri contentUri = Uri.fromFile(imageFile);
		mediaScanIntent.setData(contentUri);
		Activity activity = TiApplication.getAppCurrentActivity();
		activity.sendBroadcast(mediaScanIntent);
	}

	public static File createGalleryImageFile() {
		File pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		String appName = TiApplication.getInstance().getAppInfo().getName();
		File appPictureDir = new File(pictureDir, appName);
		if (!appPictureDir.exists()) {
			if (!appPictureDir.mkdirs()) {
				Log.e(TAG, "Failed to create application gallery directory.");
				return null;
			}
		}

		File imageFile;
		try {
			imageFile = File.createTempFile(appName.toLowerCase(), ".jpg", appPictureDir);
		} catch (IOException e) {
			Log.e(TAG, "Failed to create gallery image file: " + e.getMessage());
			return null;
		}

		return imageFile;
	}

	private static File getTemporaryImageFile(String prefix) throws IOException {
		Activity activity = CaptureDeviceModule.getCurrentActivity();
		File cacheDir = activity.getExternalCacheDir();
		if (cacheDir == null) {
			cacheDir = activity.getCacheDir();
		}
		return File.createTempFile(prefix, "jpg", cacheDir);
	}

	private static File writeTemporaryImageFile(String prefix, TiBlob blob) throws IOException {
		File dest = CaptureDeviceModule.getTemporaryImageFile(prefix);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dest));
		bos.write(blob.getBytes());
		bos.flush();
		bos.close();
		return dest;
	}

	public static KrollDict createDictForImage(TiBlob imageData) {
		KrollDict d = new KrollDict();
		d.putCodeAndMessage(MediaModule.NO_ERROR, null);
		d.put(EVENT_PROPERTY_ORIGINAL, imageData);

		File originalFile = null;
		File preprocessedFile = null;
		try {
			originalFile = CaptureDeviceModule.writeTemporaryImageFile("original", imageData);
			Log.d(TAG, "createDictForImage: saved original to file", Log.DEBUG_MODE);

			ExifInterface originalExif = new ExifInterface(originalFile.getAbsolutePath());
			final int exifOrientation = originalExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
			Log.d(TAG, String.format("exifOrientation: %d", exifOrientation));
			boolean hasOrientation = ExifInterface.ORIENTATION_UNDEFINED != exifOrientation;
			if (!hasOrientation) {
				Log.d(TAG, "createDictForImage: NO orientation", Log.DEBUG_MODE);
			} else {
				Log.d(TAG, String.format("createDictForImage: HAS orientation:%d", exifOrientation), Log.DEBUG_MODE);
			}
			Log.d(TAG, String.format("createDictForImage: Exif image w:%d h:%d",
									 originalExif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1),
									 originalExif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1)),
				  Log.DEBUG_MODE);

			preprocessedFile = CaptureDeviceModule.getTemporaryImageFile("preprocessed");

			// down-sample enough to be able to decode it within the VM capacity
			CaptureDeviceModule.transformBitmap(originalFile, preprocessedFile, new BitmapTransformationCallback() {
					public Bitmap onTransformBitmap(Bitmap source) {
						// Create transform matrix
						Matrix matrix = new Matrix();
						// Don't rotate on our own: already rotated by the hardware according to Camera.setRotation
			            switch (exifOrientation) {
				            case 2:
				                matrix.setScale(-1, 1);
				                break;
				            case 3:
				                matrix.setRotate(180);
				                break;
				            case 4:
				                matrix.setRotate(180);
				                matrix.postScale(-1, 1);
				                break;
				            case 5:
				                matrix.setRotate(90);
				                matrix.postScale(-1, 1);
				                break;
				            case 6:
				                matrix.setRotate(90);
				                break;
				            case 7:
				                matrix.setRotate(-90);
				                matrix.postScale(-1, 1);
				                break;
				            case 8:
				                matrix.setRotate(-90);
				                break;
				            default:
				            	break;
			            }
						return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
					}
				});

			List<TagInfo> excludedFields = new ArrayList<TagInfo>();
			// Don't exclude orientation info: it's set by the hardware according to Camera.setRotation
			excludedFields.add(TiffTagConstants.TIFF_TAG_ORIENTATION);

			CaptureDeviceModule.copyExifData(originalFile, preprocessedFile, excludedFields);

			TiBlob sourceBlob = CaptureDeviceModule.imageBlobFromFile(preprocessedFile);
			int width = sourceBlob.getWidth();
			int height = sourceBlob.getHeight();
			float contentWidth;
			float contentHeight;
			float thumbnailWidth;
			float thumbnailHeight;
			if(width < height){
				contentWidth = PHOTO_WIDTH_CONTENT;
				contentHeight = height * PHOTO_WIDTH_CONTENT / width;
				thumbnailWidth = PHOTO_WIDTH_THUMBNAIL;
				thumbnailHeight = height * PHOTO_WIDTH_THUMBNAIL / width;
			}else{
				contentWidth = width * PHOTO_WIDTH_CONTENT / height;
				contentHeight = PHOTO_WIDTH_CONTENT;
				thumbnailWidth = width * PHOTO_WIDTH_THUMBNAIL / height;
				thumbnailHeight = PHOTO_WIDTH_THUMBNAIL;
			}
			d.put(EVENT_PROPERTY_CONTENT,
				  CaptureDeviceModule.resizeAndCopyExifData(preprocessedFile, contentWidth, contentHeight, excludedFields));
			Log.d(TAG, "createDictForImage: resized for content", Log.DEBUG_MODE);

			d.put(EVENT_PROPERTY_THUMBNAIL,
				  CaptureDeviceModule.resizeAndCopyExifData(preprocessedFile, thumbnailWidth, thumbnailHeight, excludedFields));
			Log.d(TAG, "createDictForImage: resized for thumbnail", Log.DEBUG_MODE);
		} catch (IOException e) {
			d.putCodeAndMessage(MediaModule.UNKNOWN_ERROR, e.toString());
			Log.e(TAG, "Error while preparing image: " + e.toString());
		} finally {
			if (originalFile != null) {
				if (originalFile.exists()) originalFile.delete();
			}
			if (preprocessedFile != null) {
				if (preprocessedFile.exists()) preprocessedFile.delete();
			}
		}

		return d;
	}

	private static Activity getCurrentActivity() {
		TiApplication appContext = TiApplication.getInstance();
		return appContext.getCurrentActivity();
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

	private static TiBlob imageBlobFromFile(File file) {
		return CaptureDeviceModule.imageBlobFromFile(file, false);
	}

	private static TiBlob imageBlobFromFile(File file, boolean loadBytes) {
		TiBlob blob = TiBlob.blobFromFile(TiFileFactory.createTitaniumFile(file.getAbsolutePath(), false), "image/jpeg");
		if (loadBytes) {
			return TiBlob.blobFromData(blob.getBytes(), "image/jpeg");
		}
		return blob;
	}

	private static TiBlob resizeAndCopyExifData(File sourceFile, final Number width, final Number height, List<TagInfo> excludedFields) throws IOException {
		File resizedFile = null;
		try {
			resizedFile = CaptureDeviceModule.getTemporaryImageFile("resized");
			CaptureDeviceModule.transformBitmap(sourceFile, resizedFile, new BitmapTransformationCallback() {
					public Bitmap onTransformBitmap(Bitmap source) {
						return Bitmap.createScaledBitmap(source, width.intValue(), height.intValue(), true);
					}
				});
			CaptureDeviceModule.copyExifData(sourceFile, resizedFile, excludedFields);
			return CaptureDeviceModule.imageBlobFromFile(resizedFile, true);
		} finally {
			if (resizedFile != null) {
				if (resizedFile.exists()) resizedFile.delete();
			}
		}
	}

	private interface BitmapTransformationCallback {
		Bitmap onTransformBitmap(Bitmap source);
	}

	private static boolean transformBitmap(File inFile, File outFile, BitmapTransformationCallback callback)
        throws FileNotFoundException, IOException
	{
		// Declare
		FileInputStream inStream = null;
		FileOutputStream outStream = null;
 
		// Create options
		BitmapFactory.Options options = new BitmapFactory.Options();
 
		// Increment inSampleSize progressively to reduce image resolution and size. If
		// the program is properly managing memory, and you don't have other large images
		// loaded in memory, this loop will generally not need to go through more than 3
		// iterations. To be safe though, we stop looping after a certain amount of tries
		// to avoid infinite loops
		for (options.inSampleSize = 1; options.inSampleSize <= 32; options.inSampleSize++)
			{
				try
					{
						// Load the bitmap from file
						inStream = new FileInputStream(inFile);
						Bitmap originalBitmap = BitmapFactory.decodeStream(inStream, null, options);
 
						// Rotate the bitmap
						Bitmap transformedBitmap = callback.onTransformBitmap(originalBitmap);
 
						// Save the rotated bitmap
						outStream = new FileOutputStream(outFile);
						transformedBitmap.compress(CompressFormat.JPEG, 100, outStream);
						outStream.close();
 
						// Recycle the bitmaps to immediately free memory
						originalBitmap.recycle();
						originalBitmap = null;
						transformedBitmap.recycle();
						transformedBitmap = null;
 
						// Return
						return true;
					}
				catch (OutOfMemoryError e)
					{
						// If an OutOfMemoryError occurred, we continue with for loop and next inSampleSize value
					Log.d(TAG, "OutOfMemoryError while decoding bitmap, retyring", Log.DEBUG_MODE);
					}
				finally
					{
						// Clean-up if we failed on save
						if (outStream != null)
							{
								try
									{
										outStream.close();
									}
								catch (IOException e)
									{
									}
							}
					}
			}
 
		// Failed
		return false;
	}

	private static void copyExifData(File sourceFile, File destFile, List<TagInfo> excludedFields)
	{
		String tempFileName = destFile.getAbsolutePath() + ".tmp";
		File tempFile = null;
		OutputStream tempStream = null;
 
		try
			{
				tempFile = new File (tempFileName);
 
				TiffOutputSet sourceSet = getSanselanOutputSet(sourceFile);
				TiffOutputSet destSet = getSanselanOutputSet(destFile);
 
				destSet.getOrCreateExifDirectory();
 
				// Go through the source directories
				List<?> sourceDirectories = sourceSet.getDirectories();
				for (int i=0; i<sourceDirectories.size(); i++)
					{
						TiffOutputDirectory sourceDirectory = (TiffOutputDirectory)sourceDirectories.get(i);
						TiffOutputDirectory destinationDirectory = getOrCreateExifDirectory(destSet, sourceDirectory);
 
						if (destinationDirectory == null) continue; // failed to create
 
						// Loop the fields
						List<?> sourceFields = sourceDirectory.getFields();
						for (int j=0; j<sourceFields.size(); j++)
							{
								// Get the source field
								TiffOutputField sourceField = (TiffOutputField) sourceFields.get(j);
 
								// Check exclusion list
								if (excludedFields.contains(sourceField.tagInfo))
									{
										destinationDirectory.removeField(sourceField.tagInfo);
										continue;
									}
 
								// Remove any existing field
								destinationDirectory.removeField(sourceField.tagInfo);
 
								// Add field
								destinationDirectory.add(sourceField);
							}
					}
 
				// Save data to destination
				tempStream = new BufferedOutputStream(new FileOutputStream(tempFile));
				new ExifRewriter().updateExifMetadataLossless(destFile, tempStream, destSet);
				tempStream.close();
 
				// Replace file
				if (destFile.delete())
					{
						tempFile.renameTo(destFile);
					}
			}
		catch (ImageReadException exception)
			{
				exception.printStackTrace();
			}
		catch (ImageWriteException exception)
			{
				exception.printStackTrace();
			}
		catch (IOException exception)
			{
				exception.printStackTrace();
			}
		finally
			{
				if (tempStream != null)
					{
						try
							{
								tempStream.close();
							}
						catch (IOException e)
							{
							}
					}
 
				if (tempFile != null)
					{
						if (tempFile.exists()) tempFile.delete();
					}
			}
	}
 
	private static TiffOutputSet getSanselanOutputSet(File jpegImageFile)
        throws IOException, ImageReadException, ImageWriteException
	{
		TiffOutputSet outputSet = null;
 
		// note that metadata might be null if no metadata is found.
		IImageMetadata metadata = Sanselan.getMetadata(jpegImageFile);
		JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
		if (jpegMetadata != null)
			{
				// note that exif might be null if no Exif metadata is found.
				TiffImageMetadata exif = jpegMetadata.getExif();
 
				if (exif != null)
					{
						outputSet = exif.getOutputSet();
					}
			}
 
		// if file does not contain any exif metadata, we create an empty
		// set of exif metadata. Otherwise, we keep all of the other
		// existing tags.
		if (outputSet == null)
			outputSet = new TiffOutputSet();
 
		// Return
		return outputSet;
	}
 
	private static TiffOutputDirectory getOrCreateExifDirectory(TiffOutputSet outputSet, TiffOutputDirectory outputDirectory)
	{
		TiffOutputDirectory result = outputSet.findDirectory(outputDirectory.type);
		if (result != null)
			return result;
		result = new TiffOutputDirectory(outputDirectory.type);
		try
			{
				outputSet.addDirectory(result);
			}
		catch (ImageWriteException e)
			{
				return null;
			}
		return result;
	}
}
