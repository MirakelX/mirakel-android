/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class FileUtils {
	private static final String TAG = "FileUtils";
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;

	public static String getPath(Context context, Uri uri)
			throws URISyntaxException {
		if (uri == null)
			return null;
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			String[] projection = { "_data" };
			Cursor cursor = null;

			try {
				cursor = context.getContentResolver().query(uri, projection,
						null, null, null);
				int column_index = cursor.getColumnIndexOrThrow("_data");
				if (cursor.moveToFirst()) {
					return cursor.getString(column_index);
				}
			} catch (Exception e) {
				// Eat it
			}
		} else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	/**
	 * Copy File
	 * 
	 * @param src
	 * @param dst
	 * @throws IOException
	 */
	@SuppressWarnings("resource")
	public static void copyFile(File src, File dst) throws IOException {
		FileChannel inChannel = new FileInputStream(src).getChannel();
		FileChannel outChannel = new FileOutputStream(dst).getChannel();
		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
		}
	}

	/**
	 * Unzip a File and Copy it to a location
	 * 
	 * @param zipFile
	 * @param location
	 */
	public static void unzip(File zipFile, File location)
			throws FileNotFoundException, IOException {

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

	public static void writeToFile(File f, String s) {
		try {
			if (f.exists())
				f.delete();
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write(s);
			out.close();
		} catch (IOException e) {
			Log.e(TAG, "cannot write to file");
			Log.e(TAG, Log.getStackTraceString(e));
		}

	}

	static public String getPathFromUri(Uri uri, Context ctx) {
		try {
			return getPath(ctx, uri);
		} catch (URISyntaxException e) {
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
		if (file == null)
			return null;
		return Uri.fromFile(file);
	}

	/** Create a File for saving an image or video */
	public static File getOutputMediaFile(int type) {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.
	
		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"Mirakel");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.
	
		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}
	
		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
				Locale.getDefault()).format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "IMG_" + timeStamp + ".jpg");
		} else if (type == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "VID_" + timeStamp + ".mp4");
		} else {
			return null;
		}
	
		return mediaFile;
	}

}
