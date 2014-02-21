/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.azapps.mirakel.DefinitionsHelper;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class FileUtils {
	private static final String	TAG					= "FileUtils";
	public static final int		MEDIA_TYPE_IMAGE	= 1;
	public static final int		MEDIA_TYPE_VIDEO	= 2;
	public static final int		MEDIA_TYPE_AUDIO	= 3;
	private static String MIRAKEL_DIR;

	@SuppressLint("NewApi")
	public static String getPath(Context context, Uri uri) throws URISyntaxException {
		if (uri == null) return null;
		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

		// DocumentProvider
		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
			// ExternalStorageProvider
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				// final String type = split[0]; not used
				
				// Environment.
				// TODO somehow handle that here may be the diskuuid as type
				// if ("primary".equalsIgnoreCase(type)) {
				String path = Environment.getExternalStorageDirectory()
						.getPath() + "/" + split[1];
				if (new File(path).exists()) return path;
				// } else {
				// Log.d(TAG, type);
				// }

				// TODO handle non-primary volumes
			}
			// DownloadsProvider
			else if (isDownloadsDocument(uri)) {

				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(
						Uri.parse("content://downloads/public_downloads"),
						Long.valueOf(id));

				return getDataColumn(context, contentUri, null, null);
			}
			// MediaProvider
			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[] { split[1] };

				return getDataColumn(context, contentUri, selection,
						selectionArgs);
			}
		}
		// MediaStore (and general)
		else if ("content".equalsIgnoreCase(uri.getScheme())) {
			return getDataColumn(context, uri, null, null);
		}
		// File
		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}
		throw new URISyntaxException(uri.toString(),
				"dont be equal to cont || file");
	}

	/**
	 * Get the value of the data column for this Uri. This is useful for MediaStore Uris, and other
	 * file-based ContentProviders.
	 * 
	 * @param context
	 *        The context.
	 * @param uri
	 *        The Uri to query.
	 * @param selection
	 *        (Optional) Filter used in the query.
	 * @param selectionArgs
	 *        (Optional) Selection arguments used in the query.
	 * @return The value of the _data column, which is typically a file path.
	 */
	public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };

		try {
			cursor = context.getContentResolver().query(uri, projection,
					selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int column_index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(column_index);
			}
		} finally {
			if (cursor != null) cursor.close();
		}
		return null;
	}

	/**
	 * @param uri
	 *        The Uri to check.
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 */
	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri
				.getAuthority());
	}

	/**
	 * @param uri
	 *        The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 */
	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri
				.getAuthority());
	}

	/**
	 * @param uri
	 *        The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 */
	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri
				.getAuthority());
	}

	/**
	 * Copy File
	 * 
	 * @param src
	 * @param dst
	 * @throws IOException
	 */
	public static void copyFile(File src, File dst) throws IOException {
		if (!src.canRead() || !dst.canWrite()) {
			Log.e(TAG, "cannot copy file");
			return;
		}
		copyByStream(new FileInputStream(src), new FileOutputStream(dst));
	}

	public static void copyByStream(FileInputStream src, FileOutputStream dst) throws IOException {
		FileChannel inChannel = src.getChannel();
		FileChannel outChannel = dst.getChannel();
		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {
			if (inChannel != null) inChannel.close();
			if (outChannel != null) outChannel.close();
		}
	}

	/**
	 * Unzip a File and Copy it to a location
	 * 
	 * @param zipFile
	 * @param location
	 */
	public static void unzip(File zipFile, File location) throws FileNotFoundException, IOException {

		FileInputStream fin = new FileInputStream(zipFile);
		ZipInputStream zin = new ZipInputStream(fin);
		ZipEntry ze = null;
		while ((ze = zin.getNextEntry()) != null) {
			if (ze.isDirectory()) {
				dirChecker(location, ze.getName());
			} else {
				FileOutputStream fout = new FileOutputStream(new File(location,
						ze.getName()));
				for (int c = zin.read(); c != -1; c = zin.read()) {
					fout.write(c);
				}

				zin.closeEntry();
				fout.close();
			}

		}
		zin.close();

	}

	private static void dirChecker(File location, String dir) {
		File f = new File(location, dir);

		if (!f.isDirectory()) {
			f.mkdirs();
		}
	}

	public static void safeWriteToFile(File f, String s) {
		try {
			writeToFile(f, s);
		} catch (IOException e) {
			Log.e(TAG, "cannot write to file");
			Log.e(TAG, Log.getStackTraceString(e));
		}

	}

	public static void writeToFile(File f, String s) throws IOException {
		if (f.exists()) f.delete();
		BufferedWriter out = new BufferedWriter(new FileWriter(f));
		out.write(s);
		out.close();
	}

	static public String getPathFromUri(Uri uri, Context ctx) {
		try {
			String path = getPath(ctx, uri);
			Log.w(TAG, "PATH: " + path);
			return path;
		} catch (URISyntaxException e) {
			Log.w(TAG, Log.getStackTraceString(e));
			Toast.makeText(ctx, "Something terrible happened…",
					Toast.LENGTH_LONG).show();
			return "";
		}
	}

	public static String getMimeType(String url) {
		String type = null;
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		if (extension != null) {
			MimeTypeMap mime = MimeTypeMap.getSingleton();
			type = mime.getMimeTypeFromExtension(extension);
		}
		return type;
	}

	/** Create a file Uri for saving an image or video */
	public static Uri getOutputMediaFileUri(int type) {
		File file = FileUtils.getOutputMediaFile(type);
		if (file == null) return null;
		return Uri.fromFile(file);
	}

	/** Create a File for saving an image or video */
	public static File getOutputMediaFile(int type) {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = FileUtils.getMediaStorageDir();

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
				Locale.getDefault()).format(new Date());
		File mediaFile;
		switch (type) {
			case MEDIA_TYPE_IMAGE:
				mediaFile = new File(mediaStorageDir.getPath() + File.separator
						+ "IMG_" + timeStamp + ".jpg");
				break;
			case MEDIA_TYPE_VIDEO:
				mediaFile = new File(mediaStorageDir.getPath() + File.separator
						+ "VID_" + timeStamp + ".mp4");
				break;
			case MEDIA_TYPE_AUDIO:
				mediaFile = new File(mediaStorageDir.getPath() + File.separator
						+ "AUD_" + timeStamp + ".mp3");
				break;
			default:
				return null;
		}

		return mediaFile;
	}

	/**
	 * Returns the directory on the SD-Card
	 * 
	 * @return
	 */
	public static File getMediaStorageDir() {
		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"Mirakel");
		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				return null;
			}
		}
		return mediaStorageDir;
	}

	/**
	 * Returns the Mirakel dir in /data/data/
	 * 
	 * @return
	 */
	public static String getMirakelDir() {
		if (DefinitionsHelper.APK_NAME == null) // wtf
			DefinitionsHelper.APK_NAME = "de.azapps.mirakelandroid";
		if (MIRAKEL_DIR == null)
			MIRAKEL_DIR = Environment.getDataDirectory() + "/data/"
					+ DefinitionsHelper.APK_NAME + "/";
		return MIRAKEL_DIR;
	}

}
