package de.azapps.mirakel.helper;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import de.azapps.mirakel.R;

public class ExportImport {
	Context ctx;
	File dbFile = new File(Environment.getDataDirectory()
			+ "/data/de.azapps.mirakel/databases/mirakel.db");
	File exportDir = new File(Environment.getExternalStorageDirectory(), "");

	public ExportImport(Context ctx) {
		this.ctx = ctx;
	}

	public void exportDB() {
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

	public void importDB() {
		File file = new File(exportDir, dbFile.getName());

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

}
