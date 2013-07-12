package de.azapps.mirakel.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class FileUtils {
	public static String getPath(Context context, Uri uri)
			throws URISyntaxException {
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
	 * Source:
	 * "http://www.jondev.net/articles/Unzipping_Files_with_Android_%28Programmatically%29"
	 * 
	 * @author az
	 * 
	 */
	public class Decompress {

		private String zipFile;
		private String location;

		public Decompress(String zipFile, String location) {
			this.zipFile = zipFile;
			this.location = location;

			dirChecker("");
		}

		public void unzip() {
			try {
				FileInputStream fin = new FileInputStream(zipFile);
				ZipInputStream zin = new ZipInputStream(fin);
				ZipEntry ze = null;
				while ((ze = zin.getNextEntry()) != null) {
					Log.v("Decompress", "Unzipping " + ze.getName());

					if (ze.isDirectory()) {
						dirChecker(ze.getName());
					} else {
						FileOutputStream fout = new FileOutputStream(location
								+ ze.getName());
						for (int c = zin.read(); c != -1; c = zin.read()) {
							fout.write(c);
						}

						zin.closeEntry();
						fout.close();
					}

				}
				zin.close();
			} catch (Exception e) {
				Log.e("Decompress", "unzip", e);
			}

		}

		private void dirChecker(String dir) {
			File f = new File(location + dir);

			if (!f.isDirectory()) {
				f.mkdirs();
			}
		}
	}

}
