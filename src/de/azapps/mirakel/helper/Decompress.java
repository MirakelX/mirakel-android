package de.azapps.mirakel.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
