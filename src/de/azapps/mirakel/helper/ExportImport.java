package de.azapps.mirakel.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.util.SparseIntArray;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class ExportImport {
	private static final File dbFile = new File(Mirakel.MIRAKEL_DIR
			+ "databases/mirakel.db");
	private static final String TAG = "ExportImport";
	private static final File exportDir = new File(
			Environment.getExternalStorageDirectory(), "mirakel");

	public static File getBackupDir() {
		return exportDir;
	}

	@SuppressLint("SimpleDateFormat")
	public static void exportDB(Context ctx) {

		Date today = new Date();
		DateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");// SimpleDateFormat.getDateInstance();
		String filename = "mirakel_" + sdf.format(today) + ".db";
		final File file = new File(exportDir, filename);

		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
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
	public static boolean importAstrid(Context context, String path) {
		File file = new File(path);
		if (file.exists()) {
			String mimetype = Helpers.getMimeType(path);
			if (mimetype.equals("application/zip")) {
				return importAstridZip(context, file);
			} else if (mimetype.equals("text/xml")) {
				return importAstridXml(context, file);
			} else {
				Log.d(TAG, "unknown filetype");
			}
		} else {
			Log.d(TAG, "file not found");
		}
		return false;
	}

	private static boolean importAstridXml(Context context, File file) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			Log.d(TAG, "cannot configure parser");
			return false;
		}
		Document doc;
		try {
			doc = dBuilder.parse(file);
		} catch (SAXException e) {
			Log.d(TAG, "cannot parse file");
			return false;
		} catch (IOException e) {
			Log.d(TAG, "cannot read file");
			return false;
		}

		// optional, but recommended
		// read this -
		// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();

		NodeList nList = doc.getDocumentElement().getChildNodes();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		for (int i = 0; i < nList.getLength(); i++) {
			Node n = nList.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				NamedNodeMap m = n.getAttributes();
				if (m != null) {
					// Name
					String name = m.getNamedItem("title").getTextContent();
					// List
					ListMirakel list;
					Node child = null;
					if (n.getChildNodes().getLength() > 1) {
						child = n.getChildNodes().item(1);
					}
					if (child != null && child.getAttributes() != null) {
						String listname = child.getAttributes()
								.getNamedItem("value").getTextContent();
						list=ListMirakel.findByName(listname);
						if (list == null) {
							list = ListMirakel.newList(listname);
						}
					} else {
						if (settings.getBoolean("importDefaultList", false)) {
							list = ListMirakel.getList(settings.getInt("defaultImportList",
									SpecialList.firstSpecialSafe(context)
											.getId()));
							if (list == null) {
								list = SpecialList.firstSpecialSafe(context);
							}
						} else {
							list = SpecialList.firstSpecialSafe(context);
						}
					}
					Task t = Task.newTask(name, list);
					// Priority
					int prio = Integer.parseInt(m.getNamedItem("importance")
							.getTextContent());
					switch (prio) {
					case 0:
						t.setPriority(2);
						break;
					case 1:
						t.setPriority(1);
						break;
					case 2:
						t.setPriority(0);
						break;
					case 3:
						t.setPriority(-2);
						break;
					default:
						t.setPriority(0);
					}
					// Due
					long due = Long.parseLong(m.getNamedItem("dueDate")
							.getTextContent());
					GregorianCalendar d = new GregorianCalendar();
					d.setTimeInMillis(due);
					t.setDue(d);
					// Created At
					long created = Long.parseLong(m.getNamedItem("created")
							.getTextContent());
					GregorianCalendar c = new GregorianCalendar();
					c.setTimeInMillis(created);
					t.setCreatedAt(c);
					// Update At
					long update = Long.parseLong(m.getNamedItem("modified")
							.getTextContent());
					GregorianCalendar u = new GregorianCalendar();
					u.setTimeInMillis(update);
					t.setDue(u);
					// Done
					String done = m.getNamedItem("completed").getTextContent();
					t.setDone(!done.equals("0"));
					String content = m.getNamedItem("notes").getTextContent();
					t.setContent(content.trim());
					// TODO Reminder
					try {
						t.save(false);
					} catch (NoSuchListException e) {
						Log.wtf(TAG, "List did vanish");
					}

				} else {
					Log.w(TAG, "empty node");
				}
			}
		}

		return true;
	}

	@SuppressLint("SimpleDateFormat")
	private static boolean importAstridZip(Context context, File zipped) {
		File outputDir = new File(context.getCacheDir(), "astrid");
		if (!outputDir.isDirectory()) {
			outputDir.mkdirs();
		} else {
			String[] children = outputDir.list();
			for (int i = 0; i < children.length; i++) {
				new File(outputDir, children[i]).delete();
			}
		}
		try {
			FileUtils.unzip(zipped, outputDir);
		} catch (Exception e) {
			return false;
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
				try {
					t.save();
				} catch (NoSuchListException e) {
					Log.wtf(TAG, "List vanished while Import!?!?");
					Toast.makeText(context, R.string.list_vanished,
							Toast.LENGTH_LONG).show();
				}
				Log.v(TAG, "created task:" + name);

			}

		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;

		}
		return true;
	}

	public static Boolean importAnyDo(Context ctx, String path_any_do) {
		String json;
		try {
			json = getStringFromFile(path_any_do, ctx);
		} catch (IOException e) {
			Log.e(TAG, "cannot read File");
			return false;
		}
		Log.i(TAG, json);
		JsonObject i;
		try {
			i = new JsonParser().parse(json).getAsJsonObject();
		} catch (JsonSyntaxException e2) {
			Log.e(TAG, "malformed backup");
			return false;
		}
		Set<Entry<String, JsonElement>> f = i.entrySet();
		SparseIntArray listMapping = new SparseIntArray();
		List<Pair<Integer, String>> contents = new ArrayList<Pair<Integer, String>>();
		taskMapping = new SparseIntArray();
		for (Entry<String, JsonElement> e : f) {
			if (e.getKey().equals("categorys")) {
				Iterator<JsonElement> iter = e.getValue().getAsJsonArray()
						.iterator();
				while (iter.hasNext()) {
					listMapping = parseAnyDoList(iter.next().getAsJsonObject(),
							listMapping);
				}
			} else if (e.getKey().equals("tasks")) {
				Iterator<JsonElement> iter = e.getValue().getAsJsonArray()
						.iterator();
				while (iter.hasNext()) {
					contents = parseAnyDoTask(iter.next().getAsJsonObject(),
							listMapping, contents, ctx);
				}
			} else {
				Log.d(TAG, e.getKey());
			}
		}
		for (Pair<Integer, String> pair : contents) {
			Task t = Task.get(taskMapping.get(pair.first));
			if (t == null) {
				Log.d(TAG, "Task not found");
				continue;
			}
			String oldContent = t.getContent();
			t.setContent(oldContent == null || oldContent.equals("") ? pair.second
					: oldContent + "\n" + pair.second);
			try {
				t.save();
			} catch (NoSuchListException e1) {
				Log.wtf(TAG, "List did vanish");
			}
		}
		return true;
	}

	private static SparseIntArray taskMapping;

	private static List<Pair<Integer, String>> parseAnyDoTask(
			JsonObject jsonTask, SparseIntArray listMapping,
			List<Pair<Integer, String>> contents, Context ctx) {
		String name = jsonTask.get("title").getAsString();
		if (jsonTask.has("parentId")) {
			contents.add(new Pair<Integer, String>(jsonTask.get("parentId")
					.getAsInt(), name));
			return contents;
		}
		int list_id = jsonTask.get("categoryId").getAsInt();
		Task t = Task.newTask(name, ListMirakel.getList(listMapping.get(list_id)));
		taskMapping.put(jsonTask.get("id").getAsInt(), (int) t.getId());
		if (jsonTask.has("dueDate")) {
			Calendar due = new GregorianCalendar();
			long dueMs = jsonTask.get("dueDate").getAsLong();
			if (dueMs > 0) {
				due.setTimeInMillis(dueMs);
				t.setDue(due);
			}
		}
		if (jsonTask.has("priority")) {
			int prio = 0;
			if (jsonTask.get("priority").getAsString().equals("High")) {
				prio = 2;
			}
			t.setPriority(prio);
		}
		if (jsonTask.has("status")) {
			t.setDone(jsonTask.get("status").getAsString().equals("DONE"));
		}
		if (jsonTask.has("repeatMethod")) {
			String repeat = jsonTask.get("repeatMethod").getAsString();

			if (!repeat.equals("TASK_REPEAT_OFF")) {
				Recurring r=null;
				if (repeat.equals("TASK_REPEAT_DAY")) {
					r = Recurring.get(1, 0, 0);
					if (r == null) {
						r = Recurring.newRecurring(
								ctx.getString(R.string.daily), 0, 0, 1, 0, 0,
								true, null, null,false);
					}
				} else if (repeat.equals("TASK_REPEAT_WEEK")) {
					r = Recurring.get(7, 0, 0);
					if (r == null) {
						r = Recurring.newRecurring(
								ctx.getString(R.string.weekly), 0, 0, 7, 0, 0,
								true, null, null,false);
					}
				} else if (repeat.equals("TASK_REPEAT_MONTH")) {
					r = Recurring.get(0, 1, 0);
					if (r == null) {
						r = Recurring.newRecurring(
								ctx.getString(R.string.monthly), 0, 0, 0, 1, 0,
								true, null, null,false);
					}
				} else if (repeat.equals("TASK_REPEAT_YEAR")) {
					r = Recurring.get(0, 0, 1);
					if (r == null) {
						r = Recurring.newRecurring(
								ctx.getString(R.string.yearly), 0, 0, 0, 0, 1,
								true, null, null,false);
					}
				}
				if(r!=null){
					t.setRecurrence(r.getId());
				}else{
					Log.d(TAG,repeat);
				}

			}
		}
		try {
			t.save();
		} catch (NoSuchListException e) {
			Log.wtf(TAG, "list did vanish");
		}
		return contents;
	}

	private static SparseIntArray parseAnyDoList(JsonObject jsonList,
			SparseIntArray listMapping) {
		String name = jsonList.get("name").getAsString();
		int id = jsonList.get("id").getAsInt();
		ListMirakel l = ListMirakel.newList(name);
		listMapping.put(id, l.getId());
		return listMapping;

	}

	private static String getStringFromFile(String path, Context ctx)
			throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		StringBuilder sb = new StringBuilder();
		String line = br.readLine();

		while (line != null) {
			sb.append(line);
			sb.append('\n');
			line = br.readLine();
		}
		br.close();
		return sb.toString();
	}
}
