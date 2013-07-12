package de.azapps.mirakel.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import de.azapps.mirakel.R;

public class Backup {
	Context ctx;
	File dbFile = new File(Environment.getDataDirectory()
			+ "/data/de.azapps.mirakel/databases/mirakel.db");
	File exportDir = new File(Environment.getExternalStorageDirectory(), "");

	public Backup(Context ctx) {
		this.ctx = ctx;
	}

	public void exportDB() {
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
		File file = new File(exportDir, dbFile.getName());

		try {
			file.createNewFile();
			copyFile(dbFile, file);
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
			copyFile(file, dbFile);
			Toast.makeText(ctx, ctx.getString(R.string.backup_import_ok),
					Toast.LENGTH_LONG).show();
			android.os.Process.killProcess(android.os.Process.myPid());
		} catch (IOException e) {
			Log.e("mypck", e.getMessage(), e);
			Toast.makeText(ctx, ctx.getString(R.string.backup_import_error),
					Toast.LENGTH_LONG).show();
		}
	}

	void copyFile(File src, File dst) throws IOException {
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

}
