package de.azapps.mirakel.helper;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import de.azapps.mirakel.R;

public class ExportImport {
	private static final File dbFile = new File(Environment.getDataDirectory()
			+ "/data/de.azapps.mirakel/databases/mirakel.db");

	public static void exportDB(Context ctx, File exportDir) {
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
		File file = new File(exportDir, dbFile.getName());

		try {
			file.createNewFile();
			FileUtils.copyFile(dbFile, file);
			Toast.makeText(ctx, ctx.getString(R.string.backup_export_ok),
					Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Log.e("mypck", e.getMessage(), e);
			Toast.makeText(ctx, ctx.getString(R.string.backup_export_error),
					Toast.LENGTH_LONG).show();
		}
	}

	public static void importDB(Context ctx, File file) {

		try {
			FileUtils.copyFile(file, dbFile);
			Toast.makeText(ctx, ctx.getString(R.string.backup_import_ok),
					Toast.LENGTH_LONG).show();
			android.os.Process.killProcess(android.os.Process.myPid());
		} catch (IOException e) {
			Log.e("mypck", e.getMessage(), e);
			Toast.makeText(ctx, ctx.getString(R.string.backup_import_error),
					Toast.LENGTH_LONG).show();
		}
	}

	public static void importAstrid(Activity activity, String path) {

	}

}
