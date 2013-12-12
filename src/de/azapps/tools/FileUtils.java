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
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;

public class FileUtils {
	private static final String TAG = "FileUtils";
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	public static final int MEDIA_TYPE_AUDIO = 3;

	public static String getPath(Context context, Uri uri)
			throws URISyntaxException {
		if (uri == null)
			return null;
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				String[] projection = { "_data" };
				Cursor cursor = null;

				try {
					cursor = context.getContentResolver().query(uri,
							projection, null, null, null);
					int column_index = cursor.getColumnIndexOrThrow("_data");
					if (cursor.moveToFirst()) {
						return cursor.getString(column_index);
					}
				} catch (Exception e) {
					// Eat it
				}
			} else {
				return null;
			}
		} else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}
		throw new URISyntaxException(uri.toString(),
				"dont be equal to cont || file");
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
		if (!src.canRead() || !dst.canWrite()) {
			Log.e(TAG, "cannot copy file");
			return;
		}
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

	public static void safeWriteToFile(File f, String s) {
		try {
			writeToFile(f, s);
		} catch (IOException e) {
			Log.e(TAG, "cannot write to file");
			Log.e(TAG, Log.getStackTraceString(e));
		}

	}

	public static void writeToFile(File f, String s) throws IOException {
		if (f.exists())
			f.delete();
		BufferedWriter out = new BufferedWriter(new FileWriter(f));
		out.write(s);
		out.close();
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
		if (Mirakel.APK_NAME == null)// wtf
			Mirakel.APK_NAME = "de.azapps.mirakelandroid";
		if (Mirakel.MIRAKEL_DIR == null)
			Mirakel.MIRAKEL_DIR = Environment.getDataDirectory() + "/data/"
					+ Mirakel.APK_NAME + "/";
		return Mirakel.MIRAKEL_DIR;
	}

}
