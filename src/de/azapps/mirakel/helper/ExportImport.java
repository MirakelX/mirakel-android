package de.azapps.mirakel.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;

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
		File outputDir = new File(activity.getCacheDir(), "astrid");
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
			Toast.makeText(activity, R.string.astrid_unsuccess,
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
			while ((row = listsReader.readNext()) != null) {
				String name = row[0];
				if (ListMirakel.findByName(name) == null) {
					ListMirakel.newList(name);
					Log.e("created", name);
				}
			}
			CSVReader tasksReader = new CSVReader(tasks, ',');
			tasksReader.readNext(); // Skip first line
			while ((row = tasksReader.readNext()) != null) {
				String name = row[0];
				String content = row[8];
				String listName = row[7];
				int priority = Integer.valueOf(row[5]) - 1;
				ListMirakel list = ListMirakel.findByName(listName);
				if (list == null) {
					list = ListMirakel.first();
				}
				Task t = Task.newTask(name, list);
				t.setContent(content);
				t.setPriority(priority);
				t.save();

			}

		} catch (FileNotFoundException e) {
			Toast.makeText(activity, R.string.astrid_unsuccess,
					Toast.LENGTH_LONG).show();
			return;
		} catch (IOException e) {
			Toast.makeText(activity, R.string.astrid_unsuccess,
					Toast.LENGTH_LONG).show();
			return;

		}

	}
}
