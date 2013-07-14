package de.azapps.mirakel.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;

public class ExportImport {
	private static final File dbFile = new File(Environment.getDataDirectory()
			+ "/data/de.azapps.mirakel/databases/mirakel.db");
	private static final String TAG = "ExportImport";

	public static void exportDB(Context ctx, File file) {

		try {
			file.createNewFile();
			FileUtils.copyFile(dbFile, file);
			Toast.makeText(
					ctx,
					ctx.getString(R.string.backup_export_ok,
							file.getAbsolutePath()), Toast.LENGTH_LONG).show();
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

	@SuppressLint("SimpleDateFormat")
	public static void importAstrid(Context context, String path) {
		File outputDir = new File(context.getCacheDir(), "astrid");
		if (!outputDir.isDirectory()) {
			outputDir.mkdirs();
		} else {
			String[] children = outputDir.list();
			for (int i = 0; i < children.length; i++) {
				new File(outputDir, children[i]).delete();
			}
		}
		File zipped = new File(path);
		try {
			FileUtils.unzip(zipped, outputDir);
		} catch (Exception e) {
			Toast.makeText(context, R.string.astrid_unsuccess,
					Toast.LENGTH_LONG).show();
			return;
		}
		FileReader tasks, lists;
		try {
			tasks = new FileReader(new File(outputDir, "tasks.csv"));
			lists = new FileReader(new File(outputDir, "lists.csv"));
			CSVReader listsReader = new CSVReader(lists, ',');
			String[] row;
			listsReader.readNext(); // Skip first line

			SimpleDateFormat astridFormat = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");
			while ((row = listsReader.readNext()) != null) {
				String name = row[0];
				if (ListMirakel.findByName(name) == null) {
					ListMirakel.newList(name);
					Log.v(TAG, "created list:" + name);
				}
			}
			CSVReader tasksReader = new CSVReader(tasks, ',');
			tasksReader.readNext(); // Skip first line
			while ((row = tasksReader.readNext()) != null) {
				String name = row[0];
				String content = row[8];
				String listName = row[7];
				int priority = Integer.valueOf(row[5]) - 1;

				// Due
				GregorianCalendar due = new GregorianCalendar();
				try {
					due.setTime(astridFormat.parse(row[4]));
				} catch (ParseException e) {
					due = null;
				}
				// Done
				boolean done = !row[9].equals("");

				ListMirakel list = ListMirakel.findByName(listName);
				if (list == null) {
					list = ListMirakel.first();
				}
				Task t = Task.newTask(name, list);
				t.setContent(content);
				t.setPriority(priority);
				t.setDue(due);
				t.setDone(done);
				t.save();
				Log.v(TAG, "created task:" + name);

			}

		} catch (FileNotFoundException e) {
			Toast.makeText(context, R.string.astrid_unsuccess,
					Toast.LENGTH_LONG).show();
			return;
		} catch (IOException e) {
			Toast.makeText(context, R.string.astrid_unsuccess,
					Toast.LENGTH_LONG).show();
			return;

		}

	}
}
