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
package de.azapps.mirakel.helper.export_import;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

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
import android.os.Environment;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.FileUtils;

public class ExportImport {
	private static final File dbFile = new File(FileUtils.getMirakelDir()
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
			String mimetype = FileUtils.getMimeType(path);
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
						list = ListMirakel.findByName(listname);
						if (list == null) {
							list = ListMirakel.newList(listname);
						}
					} else {
						list=MirakelPreferences.getImportDefaultList();
						if(list==null) {
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

	public static String getStringFromFile(String path, Context ctx)
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
